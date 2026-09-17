"""商家端商品管理 E2E 测试（发布→列表→上下架→编辑）"""
import subprocess
from playwright.sync_api import sync_playwright

USER_BASE = "http://localhost:8083"
MER_BASE = "http://localhost:8082"
REDIS_CLI = r"d:\SchoolShop\toolchain\redis\redis-cli.exe"
PROD_NAME = "E2E测试商品"  # 唯一标记，用于定位与清理


def captcha_answer(uuid: str) -> str:
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
                    captured["uuid"] = resp.json().get("uuid")
                except Exception:
                    pass

        page.on("response", on_response)

        # ---------- 统一登录入口 8083 -> 商家端 ----------
        page.goto(f"{USER_BASE}/login")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)
        uuid = captured.get("uuid")
        answer = captcha_answer(uuid) if uuid else ""
        page.fill("input[placeholder='账号']", "merchant")
        page.fill("input[placeholder='密码']", "admin123")
        page.fill("input[placeholder='验证码']", answer)
        page.click("button:has-text('登 录')")
        page.wait_for_url(f"{MER_BASE}/**", timeout=15000)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # ---------- 跳转商品发布页 ----------
        page.goto(f"{MER_BASE}/product/publish")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(600)
        print("publish url:", page.url)

        # ---------- 发布商品 ----------
        page.fill("input[placeholder='请输入商品名称']", PROD_NAME)
        # 选分类（el-select 点击第一个选项）
        page.click(".el-select")
        page.wait_for_timeout(500)
        page.locator(".el-select-dropdown__item:visible").first.click()
        page.wait_for_timeout(300)
        page.fill("textarea", "这是一个E2E自动化发布的测试商品描述")
        # 价格、库存
        page.locator(".el-input-number input").nth(0).fill("66.5")
        page.locator(".el-input-number input").nth(1).fill("9")
        page.screenshot(path="d:/SchoolShop/.e2e/10-publish-form.png")
        page.click("button:has-text('发布')")
        page.wait_for_timeout(1500)
        print("== publish success msg:", "发布成功" in page.inner_text("body"))

        # ---------- 商品列表页出现该商品 ----------
        page.goto(f"{MER_BASE}/product/manage")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1500)
        resp = {}
        # 重新加载以捕获列表接口
        page.reload()
        page.wait_for_timeout(1500)
        body = page.inner_text("body")
        print("== list contains E2E商品:", PROD_NAME in body)
        row = page.locator(f".el-table__body tr:has-text('{PROD_NAME}')").first
        row.wait_for(state="visible", timeout=8000)
        page.screenshot(path="d:/SchoolShop/.e2e/11-product-list.png")
        # 断言状态标签为上架
        row_text = row.inner_text()
        print("== row 上架 tag:", "上架" in row_text)

        # ---------- 下架 ----------
        row.locator("button:has-text('下架')").first.click()
        page.wait_for_timeout(500)
        page.locator(".el-message-box__btns >> button:has-text('确定')").click()
        page.wait_for_timeout(1000)
        row.wait_for(state="visible", timeout=8000)
        page.wait_for_timeout(500)
        print("== after off-shelf row 下架:", "下架" in row.inner_text())
        page.screenshot(path="d:/SchoolShop/.e2e/12-off-shelf.png")

        # ---------- 上架 ----------
        row.locator("button:has-text('上架')").first.click()
        page.wait_for_timeout(500)
        page.locator(".el-message-box__btns >> button:has-text('确定')").click()
        page.wait_for_timeout(1000)
        row.wait_for(state="visible", timeout=8000)
        page.wait_for_timeout(500)
        print("== after on-shelf row 上架:", "上架" in row.inner_text())

        # ---------- 编辑（改名称后缀） ----------
        row.locator("button:has-text('编辑')").first.click()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)
        name_input = page.locator("input[placeholder='请输入商品名称']")
        name_input.fill(f"{PROD_NAME}-改")
        page.click("button:has-text('保存')")
        page.wait_for_timeout(1200)
        print("== edit success msg:", "保存成功" in page.inner_text("body"))
        page.goto(f"{MER_BASE}/product/manage")
        page.wait_for_timeout(1500)
        print("== renamed row visible:", f"{PROD_NAME}-改" in page.inner_text("body"))
        page.screenshot(path="d:/SchoolShop/.e2e/13-after-edit.png")

        # ---------- 删除清理 ----------
        row2 = page.locator(f".el-table__body tr:has-text('{PROD_NAME}-改')").first
        row2.wait_for(state="visible", timeout=8000)
        row2.locator("button:has-text('删除')").first.click()
        page.wait_for_timeout(500)
        page.locator(".el-message-box__btns >> button:has-text('确定')").click()
        page.wait_for_timeout(1200)
        print("== after delete row gone:", f"{PROD_NAME}-改" not in page.inner_text("body"))

        browser.close()


if __name__ == "__main__":
    run()