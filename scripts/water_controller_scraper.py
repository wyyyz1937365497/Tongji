#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
同济控水器数据抓取脚本 v3：
1. Playwright 浏览器完成登录并提取必要参数
2. 关闭浏览器，避免死锁和资源占用
3. 用纯 HTTP (httpx) 完成后续 API 调用

运行：
  python -m pip install playwright cryptography httpx
  python -m playwright install chromium
  python scripts/water_controller_scraper.py
"""
from __future__ import annotations

import argparse
import base64
import csv
import json
import os
import re
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional
from urllib.parse import parse_qs, urlencode, urlparse

from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

KS_ORIGIN = "https://ks.tongji.edu.cn"
PAY_ORIGIN = "https://pay-yikatong.tongji.edu.cn"

STATUS_MAP = {
    0: "离线",
    1: "空闲",
    2: "加锁",
    3: "报警",
    4: "使用中",
}


def aes_ecb_pkcs7_encrypt_b64(obj: Dict[str, Any], key_b64: str) -> str:
    plaintext = json.dumps(obj, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    key = base64.b64decode(key_b64)
    pad_len = 16 - (len(plaintext) % 16)
    padded = plaintext + bytes([pad_len]) * pad_len
    encryptor = Cipher(algorithms.AES(key), modes.ECB()).encryptor()
    ciphertext = encryptor.update(padded) + encryptor.finalize()
    return base64.b64encode(ciphertext).decode("ascii")


def safe_close_context(context):
    try:
        context.close()
    except Exception:
        pass


def run_browser_login(agent_type: str, timeout_sec: int) -> Dict[str, Any]:
    """启动浏览器让用户登录，返回提取到的所有参数。"""
    from playwright.sync_api import sync_playwright

    print("=" * 60)
    print("Phase 1: 浏览器登录")
    print("=" * 60)
    print("请在弹出的浏览器里完成同济 IAM / 企业微信授权登录。")
    print("登录成功并进入水控器页面后，脚本会自动提取参数并关闭浏览器。\n")

    result: Dict[str, Any] = {
        "cookies": {},
        "account": None,
        "aes_key": None,
        "password": None,
        "success": False,
    }

    with sync_playwright() as p:
        context = p.chromium.launch_persistent_context(
            user_data_dir=".pw-water-profile",
            headless=False,
            viewport={"width": 430, "height": 900},
            is_mobile=True,
            has_touch=True,
            locale="zh-CN",
        )
        page = context.pages[0] if context.pages else context.new_page()
        sso_data = {}
        captured_token = {}

        def on_response(response):
            url = response.url
            if "synjones-auth" in url and "pay-yikatong.tongji.edu.cn" in url:
                qs = parse_qs(urlparse(url).query)
                if "synjones-auth" in qs:
                    captured_token["value"] = qs["synjones-auth"][0]
                    print(f"  [LISTENER] 从响应 URL 捕获 synjones-auth")
            if "/SsoCheck" in url and response.status == 200:
                try:
                    sso_data["json"] = response.json()
                except Exception as exc:
                    sso_data["error"] = str(exc)

        def on_frame_navigated(frame):
            try:
                url = frame.url
                if "synjones-auth" in url and "pay-yikatong.tongji.edu.cn" in url:
                    qs = parse_qs(urlparse(url).query)
                    if "synjones-auth" in qs:
                        captured_token["value"] = qs["synjones-auth"][0]
                        print(f"  [FRAME] 从导航 URL 捕获 synjones-auth")
            except Exception:
                pass

        page.on("response", on_response)
        page.on("framenavigated", on_frame_navigated)

        plat_url = f"{PAY_ORIGIN}/plat?loginFrom=h5"
        target = f"{PAY_ORIGIN}/berserker-base/redirect?" + urlencode({"type": "url", "url": plat_url})
        login_url = f"{PAY_ORIGIN}/berserker-auth/cas/redirect/bamboocloud?" + urlencode(
            {"targetUrl": target}
        )

        print(f"[1/4] 打开登录页面...")
        page.goto(login_url, wait_until="domcontentloaded", timeout=timeout_sec * 1000)

        print(f"[2/4] 等待 pay 平台登录态 (最长 {timeout_sec} 秒)...")
        pay_token = None
        try:
            # 使用事件驱动的 wait_for_url 替代会阻塞事件循环的 time.sleep 轮询
            page.wait_for_url(
                lambda url: "synjones-auth" in url and "pay-yikatong.tongji.edu.cn" in url,
                timeout=timeout_sec * 1000,
            )
            pay_token = parse_qs(urlparse(page.url).query)["synjones-auth"][0]
            print(f"  [POLL] 从 page.url 检测到 synjones-auth")
        except Exception:
            print("❌ 超时：没有检测到 pay 平台登录态。")
            safe_close_context(context)
            return result

        print("✅ 已获取 pay 平台 token。")

        print(f"[3/4] 跳转到智能控水应用...")
        redirect_url = (
            f"{PAY_ORIGIN}/berserker-base/redirect?"
            + urlencode(
                {
                    "appId": "240",
                    "type": "app",
                    "synjones-auth": pay_token,
                    "synAccessSource": agent_type,
                    "loginFrom": agent_type,
                }
            )
        )
        page.goto(redirect_url, wait_until="domcontentloaded", timeout=60000)

        print("  等待跳转到 ks.tongji.edu.cn...")
        for _ in range(30):
            if "ks.tongji.edu.cn" in page.url:
                print(f"  已到达: {page.url[:80]}...")
                break
            # 此处也使用 wait_for_timeout 避免阻塞事件循环
            page.wait_for_timeout(1000)
        else:
            print("❌ 未能跳转到 ks.tongji.edu.cn")
            safe_close_context(context)
            return result

        print(f"[4/4] 等待水控页面加载并提取参数...")
        try:
            page.wait_for_load_state("networkidle", timeout=20000)
        except Exception:
            pass
        page.wait_for_timeout(3000)

        account = None
        if "json" in sso_data:
            user = sso_data["json"].get("User") or sso_data["json"].get("user") or {}
            account = user.get("ACCOUNT") or user.get("account")

        if not account:
            try:
                account = page.evaluate("() => sessionStorage.getItem('ano')")
            except Exception:
                pass

        if not account:
            print("❌ 未能获取一卡通账号。")
            safe_close_context(context)
            return result

        result["account"] = account
        print(f"✅ 账号: {account[:2]}****{account[-2:]}")

        cookies = context.cookies()
        ks_cookies = {}
        for c in cookies:
            domain = c.get("domain") or ""
            if "ks.tongji.edu.cn" in domain:
                name = c.get("name")
                value = c.get("value")
                if name is not None:
                    ks_cookies[name] = value or ""

        if not ks_cookies:
            for c in cookies:
                domain = c.get("domain")
                if domain:
                    name = c.get("name")
                    value = c.get("value")
                    if name is not None:
                        ks_cookies[name] = value or ""

        result["cookies"] = ks_cookies
        print(f"✅ Cookies: {list(ks_cookies.keys())}")

        js_texts = []
        try:
            scripts = page.evaluate(
                "() => Array.from(document.scripts).map(s => s.src).filter(Boolean)"
            )
            resources = page.evaluate(
                """() => performance.getEntriesByType('resource')
                .map(e => e.name)
                .filter(name => name.endsWith('.js'))"""
            )
            urls = []
            seen = set()
            for url in (scripts or []) + (resources or []):
                if isinstance(url, str) and url.startswith(KS_ORIGIN) and ".js" in url and url not in seen:
                    seen.add(url)
                    urls.append(url)

            for url in urls:
                try:
                    r = context.request.get(url)
                    if r.ok:
                        t = r.text()
                        if t:
                            js_texts.append(t)
                except Exception:
                    pass
        except Exception as e:
            print(f"  [WARN] 获取 JS 失败: {e}")

        if not js_texts:
            print("❌ 未能获取水控前端 JS。")
            safe_close_context(context)
            return result

        aes_key = None
        for text in js_texts:
            for m in re.finditer(r"""["']([A-Za-z0-9+/]{22}==)["']""", text):
                candidate = m.group(1)
                try:
                    raw = base64.b64decode(candidate, validate=True)
                    if len(raw) in (16, 24, 32):
                        around = text[max(0, m.start() - 500): m.end() + 1000]
                        if "AES.encrypt" in around or "AES.decrypt" in around:
                            aes_key = candidate
                            break
                except Exception:
                    pass
            if aes_key:
                break

        password = None
        for text in js_texts:
            for m in re.finditer(r"""userpassword\s*:\s*["']([^"']{6,100})["']""", text):
                c = m.group(1)
                if not c.lower().startswith("string"):
                    password = c
                    break
            if password:
                break

        if not aes_key or not password:
            print(f"❌ 解析参数失败: aes_key={aes_key is not None}, password={password is not None}")
            safe_close_context(context)
            return result

        result["aes_key"] = aes_key
        result["password"] = password
        result["success"] = True
        print("✅ 已提取 AES key 和 password。")

        state_file = ".pw-water-state.json"
        try:
            context.storage_state(path=state_file)
            print(f"✅ 登录态已保存到 {state_file}")
        except Exception as e:
            print(f"  [WARN] 保存登录态失败: {e}")

        safe_close_context(context)
        print("✅ 浏览器已关闭。\n")
        return result


def http_fetch_json(
    client, path: str, params: Optional[Dict[str, Any]] = None, cookies: Optional[Dict[str, str]] = None
) -> Dict[str, Any]:
    import httpx

    url = f"{KS_ORIGIN}{path}"
    headers = {
        "Accept": "application/json, text/plain, */*",
        "Referer": f"{KS_ORIGIN}/",
        "User-Agent": "Mozilla/5.0 (Linux; Android 15; OPD2407) AppleWebKit/537.36 Chrome/130.0.6723.58",
    }
    r = client.get(url, params=params or {}, headers=headers, cookies=cookies, follow_redirects=True)
    r.raise_for_status()
    return r.json()


def run_http_phase(
    params: Dict[str, Any],
    group_no: Optional[str],
    group_name: Optional[str],
    output_dir: Path,
):
    import httpx

    print("=" * 60)
    print("Phase 2: HTTP API 数据抓取")
    print("=" * 60)

    account = params["account"]
    aes_key = params["aes_key"]
    password = params["password"]
    cookies = params["cookies"]

    with httpx.Client(timeout=30.0) as client:
        # 1. 获取分组列表
        print("\n[1/3] 获取控水器分组...")
        groups_resp = http_fetch_json(client, "/waterapi/api/UseHzWatch", cookies=cookies)
        if groups_resp.get("RetNo") != 0:
            raise RuntimeError(f"UseHzWatch 失败: {groups_resp.get('RetDsp')}")

        groups = groups_resp.get("List") or []
        print(f"✅ 共 {len(groups)} 个分组")

        # 过滤
        selected = groups
        if group_no:
            selected = [g for g in selected if str(g.get("ClassNo")) == str(group_no)]
        if group_name:
            selected = [g for g in selected if group_name in str(g.get("ClassName", ""))]

        if not selected:
            raise RuntimeError("过滤后分组为空。")
        print(f" 选中 {len(selected)} 个分组")

        # 2. 获取水控 token
        print("\n[2/3] 获取水控 API token...")
        now = datetime.now().strftime("%Y%m%d%H%M%S")
        info = aes_ecb_pkcs7_encrypt_b64(
            {"userid": account, "userpassword": password, "time": now},
            aes_key,
        )
        token_resp = http_fetch_json(client, "/waterapi/api/GetToken", {"info": info}, cookies=cookies)
        if token_resp.get("RetNo") != 0:
            raise RuntimeError(f"GetToken 失败: {token_resp.get('RetDsp')}")

        water_token = token_resp.get("Token")
        if not water_token:
            raise RuntimeError("GetToken 未返回 Token。")
        print("✅ Token 获取成功。")

        # 3. 逐个分组获取控水器
        print(f"\n[3/3] 抓取控水器数据...")
        group_results = []
        all_rows = []
        for idx, group in enumerate(selected, 1):
            g_no = group.get("ClassNo")
            g_name = group.get("ClassName")
            print(f" [{idx}/{len(selected)}] {g_name} ({g_no})...", end=" ")

            info2 = aes_ecb_pkcs7_encrypt_b64({"ano": account, "groupid": g_no}, aes_key)
            data = http_fetch_json(
                client,
                "/waterapi/api/AccUseHzWatch",
                {"info": info2, "token": water_token},
                cookies=cookies,
            )
            group_results.append(data)

            if data.get("RetNo") != 0:
                print(f"失败: {data.get('RetDsp')}")
                continue

            devices = data.get("List") or []
            print(f"{len(devices)} 个控水器")

            for dev in devices:
                status_code = dev.get("PosNum")
                try:
                    status_int = int(status_code)
                except Exception:
                    status_int = -1

                all_rows.append({
                    "group_no": g_no,
                    "group_name": g_name,
                    "device_no": dev.get("ClassNo"),
                    "device_name": dev.get("ClassName"),
                    "status_code": status_code,
                    "status_text": STATUS_MAP.get(status_int, "未知"),
                    "warn_pos_num": dev.get("WarnPosNum"),
                    "use_free_rate": dev.get("UseFreeRate"),
                    "book_rate": dev.get("BookRate"),
                    "actkind": dev.get("Actkind"),
                    "book_code": dev.get("BookCode"),
                })

    # 保存结果
    output_dir.mkdir(parents=True, exist_ok=True)
    metadata = {
        "fetched_at": datetime.now().isoformat(timespec="seconds"),
        "account_mask": f"{account[:2]}****{account[-2:]}",
        "group_count_all": len(groups),
        "group_count_selected": len(selected),
        "controller_count": len(all_rows),
    }

    (output_dir / "water_groups.json").write_text(
        json.dumps({"metadata": metadata, "groups": groups}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    (output_dir / "water_controllers.json").write_text(
        json.dumps({
            "metadata": metadata,
            "selected_groups": selected,
            "group_results": group_results,
            "controllers": all_rows,
        }, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    csv_path = output_dir / "water_controllers.csv"
    fieldnames = [
        "group_no",
        "group_name",
        "device_no",
        "device_name",
        "status_code",
        "status_text",
        "warn_pos_num",
        "use_free_rate",
        "book_rate",
        "actkind",
        "book_code",
    ]
    with csv_path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(all_rows)

    print(f"\n✅ 结果已保存到 {output_dir}/")
    print(f"   water_groups.json - {len(groups)} 个分组")
    print(f"   water_controllers.json - {len(all_rows)} 个控水器")
    print(f"   water_controllers.csv - 同上")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="同济智能控水数据抓取 (浏览器登录 + HTTP 抓取)")
    parser.add_argument("--output-dir", default="water_output", help="输出目录")
    parser.add_argument("--agent-type", default=os.getenv("AGENT_TYPE", "wechat-work"), help="agent 类型")
    parser.add_argument("--timeout", type=int, default=180, help="浏览器登录超时秒数")
    parser.add_argument("--group-no", default=None, help="只抓指定分组编号")
    parser.add_argument("--group-name", default=None, help="只抓分组名包含该文本的")
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    # Phase 1: 浏览器登录提取参数
    params = run_browser_login(args.agent_type, args.timeout)
    if not params.get("success"):
        print("\n❌ 登录阶段失败，退出。")
        sys.exit(1)

    # Phase 2: HTTP 抓数据
    try:
        run_http_phase(params, args.group_no, args.group_name, Path(args.output_dir))
    except Exception as e:
        print(f"\n❌ HTTP 阶段失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
