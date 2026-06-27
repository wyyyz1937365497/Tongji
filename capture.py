import asyncio
import json
import sys
import tty
import termios
import threading
from datetime import datetime
from urllib.parse import urlparse, parse_qs
from playwright.async_api import async_playwright

should_stop = False


def listen_for_q():
    """后台线程：监听终端单字符输入，按 q 或 Ctrl+C 退出"""
    global should_stop
    fd = sys.stdin.fileno()
    old_settings = termios.tcgetattr(fd)
    try:
        tty.setraw(fd)
        print("按 'q' 键停止录制并保存...\n", flush=True)
        while not should_stop:
            ch = sys.stdin.read(1)
            if ch.lower() == 'q' or ch == '\x03':
                should_stop = True
                break
    finally:
        termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)


async def main():
    captured = []          # 完整结构化记录
    captured_summary = []  # 仅摘要，用于打印与 .log 文件

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=False)
        context = await browser.new_context()
        page = await context.new_page()

        async def on_response(response):
            request = response.request

            # ---- 一级过滤：只看 xhr / fetch ----
            if request.resource_type not in ("xhr", "fetch"):
                return

            # ---- 二级过滤：响应必须是 JSON ----
            content_type = response.headers.get("content-type", "")
            if "application/json" not in content_type:
                return

            # 解析 URL
            parsed = urlparse(response.url)
            query_params = {k: v[0] if len(v) == 1 else v
                            for k, v in parse_qs(parsed.query).items()}

            # 解析响应体
            try:
                body_json = await response.json()
            except Exception:
                try:
                    body_json = await response.text()
                except Exception:
                    body_json = None

            entry = {
                "timestamp": datetime.now().isoformat(),
                "request": {
                    "method": request.method,
                    "url": response.url,
                    "path": parsed.path,
                    "query_params": query_params,
                    "headers": dict(request.headers),
                    "post_data": request.post_data,   # POST 请求体
                },
                "response": {
                    "status": response.status,
                    "status_text": response.status_text,
                    "headers": dict(response.headers),
                    "content_type": content_type,
                    "body": body_json,                # 已解析的 JSON 对象
                },
            }
            captured.append(entry)

            # 终端实时打印摘要
            summary = (f"[{entry['timestamp'][11:19]}] "
                       f"{request.method:6s} {response.status} "
                       f"{parsed.path}  params={query_params}")
            captured_summary.append(summary)
            print(summary)
            # 打印响应 JSON 前若干字符，方便肉眼核对
            if body_json is not None:
                preview = json.dumps(body_json, ensure_ascii=False)
                print(f"           ↳ {preview[:200]}{'...' if len(preview) > 200 else ''}")

        page.on("response", on_response)

        # 打开初始页面（可改成你要测试的站点）
        await page.goto("https://all.tongji.edu.cn/new/index.html#/hall",
                        wait_until="domcontentloaded")
        print("浏览器已打开，请自由操作。仅记录 JSON 接口请求。\n")

        # 持续运行直到按 q 或浏览器被关闭
        while not should_stop:
            await asyncio.sleep(0.5)
            if not browser.is_connected():
                print("\n检测到浏览器已关闭。")
                break

        print("\n正在保存数据...")
        try:
            await browser.close()
        except Exception:
            pass

    # 保存结构化 JSON
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    json_file = f"api_capture_{ts}.json"
    with open(json_file, "w", encoding="utf-8") as f:
        json.dump(captured, f, ensure_ascii=False, indent=2)

    # 保存人类可读摘要
    log_file = f"api_capture_{ts}.log"
    with open(log_file, "w", encoding="utf-8") as f:
        f.write("\n".join(captured_summary))

    print(f"✅ 共捕获 {len(captured)} 个 JSON 接口请求")
    print(f"   结构化数据：{json_file}")
    print(f"   摘要日志  ：{log_file}")


if __name__ == "__main__":
    threading.Thread(target=listen_for_q, daemon=True).start()
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n强制退出。")
    finally:
        should_stop = True
        print()
