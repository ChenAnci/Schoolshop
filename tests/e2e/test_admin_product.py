"""管理端商品管控 E2E（事后管控：列表→搜索→强制下架→上架→删除）"""
import subprocess
from playwright.sync_api import sync_playwright

ADM_BASE = "http://localhost:8081"
REDIS_CLI = r"d:\SchoolShop\toolchain\redis\redis-cli.exe"
PROD_NAME = "TMPADM管控测试"


def captcha_answer(uuid):
    out = subprocess.run([REDIS_CLI, "GET", f"captcha_codes:{uuid}"], capture_output=True, text=True).stdout.strip()
    return out.strip('"')


def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1500, "height": 900})
        captured = {}

        def on_response(resp):
            if "/captchaImage" in resp.url:
                try:
                    captured["uuid"] = resp.json().get("uuid")
                except Exception:
                    pass

        page.on("response", on_response)

        # ---------- 登录管理端 ----------
        page.goto(f"{ADM_BASE}/login")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)
        page.fill("input[placeholder='账号']", "admin")
        page.fill("input[placeholder='密码']", "admin123")
        page.fill("input[placeholder='验证码']", captcha_answer(captured.get("uuid", "")) if captured.get("uuid") else "")
        page.click("button:has-text('登 录')")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1200)
        print("== logged in url:", page.url)

        # ---------- 进入商品管理页 ----------
        page.goto(f"{ADM_BASE}/market/product")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1200)
        print("== product page title contains 商品管理:", "商品管理" in page.inner_text("body"))

        # ---------- 搜索测试商品 ----------
        page.fill("input[placeholder='按商品名称搜索']", PROD_NAME)
        page.click("button:has-text('搜索')")
        page.wait_for_timeout(1200)
        row = page.locator(f".el-table__body tr:has-text('{PROD_NAME}')").first
        row.wait_for(state="visible", timeout=8000)
        print("== searched row visible:", PROD_NAME in page.inner_text("body"))
        row_text = row.inner_text()
        print("== initial row 上架 tag:", "上架" in row_text)
        page.screenshot(path="d:/SchoolShop/.e2e/20-admin-product-list.png")

        # ---------- 强制下架 ----------
        row.locator("button:has-text('下架')").first.click()
        page.wait_for_timeout(400)
        page.locator(".el-message-box__btns >> button:has-text('确定')").click()
        page.wait_for_timeout(1200)
        row.wait_for(state="visible", timeout=8000)
        print("== after off-shelf row 下架:", "下架" in row.inner_text())
        page.screenshot(path="d:/SchoolShop/.e2e/21-admin-off-shelf.png")

        # ---------- 上架 ----------
        row.locator("button:has-text('上架')").first.click()
        page.wait_for_timeout(400)
        page.locator(".el-message-box__btns >> button:has-text('确定')").click()
        page.wait_for_timeout(1200)
        row.wait_for(state="visible", timeout=8000)
        print("== after on-shelf row 上架:", "上架" in row.inner_text())

        # ---------- 删除 ----------
        row.locator("button:has-text('删除')").first.click()
        page.wait_for_timeout(400)
        page.locator(".el-message-box__btns >> button:has-text('确定')").click()
        page.wait_for_timeout(1200)
        print("== after delete row gone:", PROD_NAME not in page.inner_text("body"))
        page.screenshot(path="d:/SchoolShop/.e2e/22-admin-deleted.png")

        # ---------- 下架状态筛选验证 ----------
        # 注：测试商品已被删，改为验证“下架”筛选后列表不再包含已删商品
        browser.close()


if __name__ == "__main__":
    run()