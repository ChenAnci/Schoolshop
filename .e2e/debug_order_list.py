"""调试：商家订单列表请求在浏览器中的真实行为"""
import subprocess
from playwright.sync_api import sync_playwright

USER_BASE = "http://localhost:8083"
MER_BASE = "http://localhost:8082"
REDIS_CLI = r"d:\SchoolShop\toolchain\redis\redis-cli.exe"


def captcha_answer(uuid):
    out = subprocess.run([REDIS_CLI, "GET", f"captcha_codes:{uuid}"], capture_output=True, text=True).stdout.strip()
    return out.strip('"')


def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        captured = {}

        def on_response(resp):
            if "/captchaImage" in resp.url:
                try:
                    data = resp.json()
                    captured["uuid"] = data.get("uuid")
                except Exception:
                    pass

        page.on("response", on_response)
        page.goto(f"{USER_BASE}/login")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)
        uuid = captured.get("uuid")
        page.fill("input[placeholder='账号']", "merchant")
        page.fill("input[placeholder='密码']", "admin123")
        page.fill("input[placeholder='验证码']", captcha_answer(uuid) if uuid else "")
        page.click("button:has-text('登 录')")
        page.wait_for_url(f"{MER_BASE}/**", timeout=15000)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1000)

        # 打印 localStorage token
        token = page.evaluate("() => Object.keys(localStorage)")
        print("localStorage keys:", token)

        # 监听 order list 请求与响应
        order_resp = {}

        def on_order_response(resp):
            if "/market/shop/order" in resp.url and "list" in resp.url:
                try:
                    order_resp["status"] = resp.status
                    order_resp["body"] = resp.text()[:800]
                    order_resp["url"] = resp.url
                except Exception as e:
                    order_resp["err"] = str(e)

        page.on("response", on_order_response)
        page.goto(f"{MER_BASE}/order/manage")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        print("order resp:", order_resp)
        browser.close()


if __name__ == "__main__":
    run()
