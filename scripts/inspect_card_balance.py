from playwright.sync_api import sync_playwright
import json
import sys

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        context = browser.new_context(
            viewport={"width": 1280, "height": 800}
        )
        page = context.new_page()

        # 监听所有请求和响应
        def handle_request(request):
            url = request.url
            if "card_balance" in url or "card" in url:
                print(f"\n{'='*60}")
                print(f"[REQUEST] {request.method} {url}")
                headers = request.headers
                print(f"  Headers:")
                for k, v in headers.items():
                    print(f"    {k}: {v}")
                if request.post_data:
                    print(f"  PostData: {request.post_data}")

        def handle_response(response):
            url = response.url
            if "card_balance" in url:
                print(f"\n{'='*60}")
                print(f"[RESPONSE] {response.status} {url}")
                try:
                    body = response.json()
                    print(f"  Body: {json.dumps(body, ensure_ascii=False, indent=2)}")
                except Exception:
                    try:
                        print(f"  Body: {response.text()[:500]}")
                    except Exception as e:
                        print(f"  Body read error: {e}")

        page.on("request", handle_request)
        page.on("response", handle_response)

        # 导航到 all 门户首页
        portal_url = "https://all.tongji.edu.cn/CUS_TEMPLATE_TONGJI/pc/index.html"
        print(f"正在打开: {portal_url}")
        print("请完成登录操作，登录后余额请求会自动触发。")
        print("按 Ctrl+C 或在浏览器中关闭页面来结束。\n")

        page.goto(portal_url)

        # 保持运行直到用户中断
        try:
            while True:
                page.wait_for_timeout(1000)
        except KeyboardInterrupt:
            print("\n用户中断")
        finally:
            browser.close()

if __name__ == "__main__":
    main()
