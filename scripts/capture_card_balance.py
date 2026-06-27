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


async def main():
    records = []

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=False)
        context = await browser.new_context(viewport={"width": 1280, "height": 800})
        page = await context.new_page()

        async def on_response(response):
            request = response.request
            if request.resource_type not in ("xhr", "fetch"):
                return
            url = response.url
            if "card_balance" not in url and "/custom/api/v1/rt/card/" not in url:
                return

            content_type = response.headers.get("content-type", "")
            try:
                body_json = await response.json()
            except Exception:
                try:
                    body_json = await response.text()
                except Exception:
                    body_json = None

            # 获取当前所有 cookie
            cookies = await context.cookies()
            tongji_cookies = [
                {"name": c.get("name"), "value": c.get("value"), "domain": c.get("domain")}
                for c in cookies if "tongji" in (c.get("domain") or "")
            ]

            entry = {
                "timestamp": datetime.now().isoformat(),
                "request": {
                    "method": request.method,
                    "url": url,
                    "headers": dict(request.headers),
                    "post_data": request.post_data,
                },
                "response": {
                    "status": response.status,
                    "headers": dict(response.headers),
                    "content_type": content_type,
                    "body": body_json,
                },
                "cookies": tongji_cookies,
            }
            records.append(entry)

            print(f"\n[CAPTURED] {request.method} {response.status} {url}")
            if body_json is not None:
                preview = json.dumps(body_json, ensure_ascii=False)
                print(f"  Body: {preview[:300]}{'...' if len(preview) > 300 else ''}")
            if tongji_cookies:
                print(f"  Cookies ({len(tongji_cookies)}):")
                for c in tongji_cookies:
                    val = c["value"] or ""
                    print(f"    {c['name']}={val[:80]}{'...' if len(val) > 80 else ''} (domain={c['domain']})")

        page.on("response", on_response)

        await page.goto("https://all.tongji.edu.cn/new/index.html#/hall",
                        wait_until="domcontentloaded")
        print("浏览器已打开，请登录。关闭浏览器或按 'q' 保存并退出。\n")

        while not should_stop:
            await asyncio.sleep(0.5)
            if not browser.is_connected():
                print("\n检测到浏览器已关闭。")
                break

        print("\n正在保存...")
        try:
            await browser.close()
        except Exception:
            pass

    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    out_file = f"card_balance_capture_{ts}.json"
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(records, f, ensure_ascii=False, indent=2)

    print(f"✅ 共捕获 {len(records)} 条 card 相关请求")
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
