from playwright.sync_api import sync_playwright
import json
import time

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        context = browser.new_context(viewport={"width": 1280, "height": 800})
        page = context.new_page()

        def on_response(response):
            url = response.url
            if "card_balance" in url or "/custom/api/v1/rt/card/" in url:
                print(f"\n{'='*60}")
                print(f"[RESPONSE] {response.status} {url}")
                try:
                    body = response.json()
                    print(json.dumps(body, ensure_ascii=False, indent=2))
                except Exception as e:
                    print(f"  Body read error: {e}")

                if "card_balance" in url:
                    print("\n[COOKIES for tongji domains]:")
                    for c in context.cookies():
                        domain = c.get("domain", "")
                        if "tongji" in domain:
                            val = c["value"]
                            display = val if len(val) < 120 else val[:120] + "..."
                            print(f"  {c['name']}={display} (domain={domain})")

        page.on("response", on_response)

        target_url = "https://all.tongji.edu.cn/new/index.html#/hall"
        print(f"Opening: {target_url}")
        print("Please complete login. The script will auto-capture card_balance + cookies.")
        print("Press Ctrl+C to stop.\n")

        page.goto(target_url)

        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\nStopped by user")
        finally:
            browser.close()

if __name__ == "__main__":
    main()
