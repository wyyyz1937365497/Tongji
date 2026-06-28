import asyncio
import json
import sys
import tty
import termios
import threading
from datetime import datetime
from playwright.async_api import async_playwright

should_stop = False


def listen_for_q():
    global should_stop
    fd = sys.stdin.fileno()
    old_settings = termios.tcgetattr(fd)
    try:
        tty.setraw(fd)
        print("按 'q' 键停止并保存...\n", flush=True)
        while not should_stop:
            ch = sys.stdin.read(1)
            if ch.lower() == 'q' or ch == '\x03':
                should_stop = True
                break
    finally:
        termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)


async def dump_storage(context, page, label=""):
    storage = {"label": label, "timestamp": datetime.now().isoformat()}
    try:
        storage["cookies"] = await context.cookies()
    except Exception as e:
        storage["cookies_error"] = str(e)
    try:
        storage["localStorage"] = await page.evaluate("""() => {
            const items = {};
            for (let i = 0; i < localStorage.length; i++) {
                const k = localStorage.key(i);
                items[k] = localStorage.getItem(k);
            }
            return items;
        }""")
    except Exception as e:
        storage["localStorage_error"] = str(e)
    try:
        storage["sessionStorage"] = await page.evaluate("""() => {
            const items = {};
            for (let i = 0; i < sessionStorage.length; i++) {
                const k = sessionStorage.key(i);
                items[k] = sessionStorage.getItem(k);
            }
            return items;
        }""")
    except Exception as e:
        storage["sessionStorage_error"] = str(e)
    return storage


async def main():
    records: list[dict] = []
    storage_snapshots: list[dict] = []
    captured_jwt: dict = {"value": None, "source": None}

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=False)
        context = await browser.new_context(viewport={"width": 1280, "height": 800})
        page = await context.new_page()

        def is_interesting(url):
            return any(d in url for d in [
                "space.tongji.edu.cn",
                "all.tongji.edu.cn",
                "iam.tongji.edu.cn",
                "yktids.tongji.edu.cn",
                "ids.tongji.edu.cn",
                "cas.tongji.edu.cn",
                "ssocas.tongji.edu.cn",
            ])

        async def on_request(request):
            url = request.url
            if not is_interesting(url):
                return
            if any(url.endswith(ext) for ext in (".js", ".css", ".png", ".jpg", ".svg", ".woff", ".woff2", ".ico", ".gif")):
                return
            entry = {
                "timestamp": datetime.now().isoformat(),
                "type": "request",
                "method": request.method,
                "url": url,
                "headers": dict(request.headers),
                "post_data": request.post_data,
                "resource_type": request.resource_type,
            }
            records.append(entry)
            print(f"\n[REQUEST] {request.method} {url}")
            if request.post_data:
                print(f"  PostData: {request.post_data[:500]}")
            auth = request.headers.get("authorization", "")
            if auth and ("bearer" in auth.lower() or "eyJ" in auth) and len(auth) > 60:
                jwt_val = auth.split("eyJ")[0] + "eyJ" + auth.split("eyJ", 1)[1] if "eyJ" in auth else auth
                captured_jwt["value"] = jwt_val
                captured_jwt["source"] = f"request-header: {url}"
                print(f"  *** JWT captured from Authorization header ***")

        async def on_response(response):
            url = response.url
            if not is_interesting(url):
                return
            if any(url.endswith(ext) for ext in (".js", ".css", ".png", ".jpg", ".svg", ".woff", ".woff2", ".ico", ".gif")):
                return

            body_preview = None
            try:
                ct = response.headers.get("content-type", "")
                if "json" in ct or "text" in ct or "html" in ct or "javascript" in ct:
                    try:
                        body_preview = await response.text()
                        if body_preview and len(body_preview) > 3000:
                            body_preview = body_preview[:3000] + "...[truncated]"
                    except Exception:
                        body_preview = "<binary or unreadable>"
            except Exception:
                pass

            set_cookie = response.headers.get("set-cookie", "")

            entry = {
                "timestamp": datetime.now().isoformat(),
                "type": "response",
                "status": response.status,
                "url": url,
                "headers": dict(response.headers),
                "body": body_preview,
            }
            records.append(entry)

            print(f"\n[RESPONSE] {response.status} {url}")
            if body_preview and len(body_preview) < 500:
                print(f"  Body: {body_preview[:500]}")
            if set_cookie:
                print(f"  Set-Cookie: {set_cookie[:200]}")

            if body_preview and "eyJ" in body_preview and "space.tongji" in url:
                print(f"  *** Response body contains JWT pattern (eyJ) ***")

        context.on("request", on_request)
        context.on("response", on_response)

        context.on("page", lambda new_page: print(f"\n*** NEW PAGE/POPUP opened: {new_page.url} ***"))

        target_url = "https://space.tongji.edu.cn/h5/#/home"
        print(f"Opening: {target_url}")
        print("如果未登录，浏览器会自动跳转到统一身份认证。")
        print("请完成登录，脚本会自动捕获 SSO 重定向链和 JWT 获取流程。")
        print("操作完成后按 'q' 保存并退出。\n")

        try:
            await page.goto(target_url, wait_until="domcontentloaded")
        except Exception as e:
            print(f"导航警告: {e}")

        snapshot_count = 0
        while not should_stop:
            await asyncio.sleep(2)
            snapshot_count += 1
            if snapshot_count % 5 == 0:
                snap = await dump_storage(context, page, f"periodic #{snapshot_count}")
                storage_snapshots.append(snap)
                space_cookies = [
                    c for c in snap.get("cookies", [])
                    if "space.tongji" in (c.get("domain") or "")
                ]
                ls = snap.get("localStorage", {})
                jwt_items = {k: v[:80] for k, v in ls.items() if v and ("eyJ" in v or "token" in k.lower())}
                print(f"\n  [Snapshot #{snapshot_count}] space cookies={len(space_cookies)} jwt_keys={list(jwt_items.keys())}")
            if not browser.is_connected():
                print("\n检测到浏览器已关闭。")
                break

        print("\n正在保存...")
        final_snap = await dump_storage(context, page, "final")
        storage_snapshots.append(final_snap)

        try:
            await browser.close()
        except Exception:
            pass

    if captured_jwt["value"]:
        print(f"\n✅ JWT 已捕获 (来源: {captured_jwt['source']})")
        print(f"   JWT 前80字符: {captured_jwt['value'][:80]}...")

    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    out_file = f"space_capture_{ts}.json"
    output = {
        "captured_jwt": captured_jwt,
        "requests_and_responses": records,
        "storage_snapshots": storage_snapshots,
    }
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    print(f"\n✅ 共捕获 {len(records)} 条请求/响应")
    print(f"   {len(storage_snapshots)} 个存储快照")
    print(f"   已保存：{out_file}")


if __name__ == "__main__":
    threading.Thread(target=listen_for_q, daemon=True).start()
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n强制退出。")
    finally:
        should_stop = True
        print()
