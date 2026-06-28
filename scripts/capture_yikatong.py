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

        async def on_request(request):
            url = request.url
            if "yikatong.tongji.edu.cn" in url:
                entry = {
                    "timestamp": datetime.now().isoformat(),
                    "type": "request",
                    "method": request.method,
                    "url": url,
                    "headers": dict(request.headers),
                    "post_data": request.post_data,
                }
                records.append(entry)
                print(f"\n[REQUEST] {request.method} {url}")
                if request.post_data:
                    print(f"  PostData: {request.post_data[:300]}")

        async def on_response(response):
            url = response.url
            if "yikatong.tongji.edu.cn" in url:
                try:
                    body_json = await response.json()
                except Exception:
                    try:
                        body_json = await response.text()
                    except Exception as e:
                        body_json = f"<error: {e}>"

                entry = {
                    "timestamp": datetime.now().isoformat(),
                    "type": "response",
                    "status": response.status,
                    "url": url,
                    "headers": dict(response.headers),
                    "body": body_json,
                }
                records.append(entry)

                print(f"\n[RESPONSE] {response.status} {url}")
                if isinstance(body_json, str):
                    print(f"  Body: {body_json[:500]}")
                else:
                    preview = json.dumps(body_json, ensure_ascii=False)
                    print(f"  Body: {preview[:500]}{'...' if len(preview) > 500 else ''}")

                if "GetPersonTrjn" in url:
                    cookies = await context.cookies()
                    yikatong_cookies = [
                        {"name": c.get("name"), "value": c.get("value"), "domain": c.get("domain")}
                        for c in cookies if "yikatong" in (c.get("domain") or "")
                    ]
                    print(f"\n  [Yikatong Cookies at GetPersonTrjn]:")
                    for c in yikatong_cookies:
                        val = c["value"] or ""
                        print(f"    {c['name']}={val[:100]}{'...' if len(val) > 100 else ''}")

        page.on("request", on_request)
        page.on("response", on_response)

        target_url = "https://yikatong.tongji.edu.cn/user/user"
        print(f"Opening: {target_url}")
        print("Please complete login. The script will capture all yikatong requests.")
        print("Press 'q' or close browser to save and exit.\n")

        await page.goto(target_url, wait_until="domcontentloaded")

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
    out_file = f"yikatong_capture_{ts}.json"
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(records, f, ensure_ascii=False, indent=2)

    print(f"✅ 共捕获 {len(records)} 条 yikatong 请求")
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
