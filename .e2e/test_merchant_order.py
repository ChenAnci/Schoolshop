"""商家端订单处理接口 E2E 测试（接单/取货/完成 + 详情 + 状态字典）"""
import subprocess
from playwright.sync_api import sync_playwright

USER_BASE = "http://localhost:8083"
MER_BASE = "http://localhost:8082"
REDIS_CLI = r"d:\SchoolShop\toolchain\redis\redis-cli.exe"
ORDER_NO = "2026081918272764600"


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

        # ---------- 统一登录入口 8083 ----------
        page.goto(f"{USER_BASE}/login")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)
        uuid = captured.get("uuid")
        answer = captcha_answer(uuid) if uuid else ""
        print("captcha answer:", answer)

        page.fill("input[placeholder='账号']", "merchant")
        page.fill("input[placeholder='密码']", "admin123")
        page.fill("input[placeholder='验证码']", answer)
        page.click("button:has-text('登 录')")

        page.wait_for_url(f"{MER_BASE}/**", timeout=15000)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1500)
        print("after login url:", page.url)

        # ---------- 进入订单列表页 ----------
        page.goto(f"{MER_BASE}/order/manage")
        order_resp = {}

        def on_order_response(resp):
            if "/market/shop/order" in resp.url and "list" in resp.url:
                try:
                    order_resp["status"] = resp.status
                    order_resp["body"] = resp.text()[:600]
                except Exception:
                    pass

        page.on("response", on_order_response)
        page.wait_for_timeout(4000)
        print("url after goto:", page.url)
        print("order list resp:", order_resp)
        page.screenshot(path="d:/SchoolShop/.e2e/02-order-list.png", full_page=True)
        # 等待包含目标订单号的行出现（数据加载完成）；限定在列表 el-table 内，避免命中详情弹窗描述
        row = page.locator(f".el-table__body tr:has-text('{ORDER_NO}')").first
        try:
            row.wait_for(state="visible", timeout=15000)
        except Exception:
            print("ROW NOT FOUND, body contains 暂无数据:", "暂无数据" in page.inner_text("body"))
            raise
        page.wait_for_timeout(500)
        page.screenshot(path="d:/SchoolShop/.e2e/02-order-list.png", full_page=True)

        # 校验目标行状态标签为 待商家接单
        row_text = row.inner_text()
        print("== row status 待商家接单:", "待商家接单" in row_text)

        # ---------- 接单：0 -> 1 ----------
        accept_btn = row.locator("button:has-text('接单')").first
        accept_btn.click()
        page.wait_for_timeout(500)
        confirm_btn = page.locator(".el-message-box__btns >> button:has-text('确定')")
        print("confirm btn visible:", confirm_btn.is_visible())
        confirm_btn.click()
        page.wait_for_timeout(800)
        # 等待状态变为 待自提
        row.wait_for(state="visible", timeout=10000)
        page.wait_for_timeout(500)
        print("== after accept row contains 待自提:", "待自提" in row.inner_text())
        page.screenshot(path="d:/SchoolShop/.e2e/03-after-accept.png", full_page=True)

        # ---------- 详情弹窗 ----------
        row.locator("button:has-text('详情')").first.click()
        page.wait_for_timeout(800)
        detail_text = page.locator(".el-dialog").inner_text()
        print("== detail modal contains 订单号:", "订单号" in detail_text)
        print("== detail modal contains 实付金额:", "实付金额" in detail_text)
        print("== detail modal contains 小计:", "小计" in detail_text)
        page.screenshot(path="d:/SchoolShop/.e2e/04-detail-modal.png")
        page.locator(".el-dialog__headerbtn").click()
        page.wait_for_timeout(400)

        # ---------- 确认取货（完成）：1 -> 2 ----------
        row.locator("button:has-text('确认取货')").first.click()
        page.wait_for_timeout(500)
        page.locator(".el-message-box__btns >> button:has-text('确定')").click()
        page.wait_for_timeout(800)
        row.wait_for(state="visible", timeout=10000)
        page.wait_for_timeout(500)
        print("== after take row contains 已完成:", "已完成" in row.inner_text())
        page.screenshot(path="d:/SchoolShop/.e2e/05-after-take.png", full_page=True)

        # ---------- 状态 Tab 筛选：待商家接单（应无数据） ----------
        page.click(".el-tabs__item:has-text('待商家接单')")
        page.wait_for_timeout(1000)
        print("== tab 待商家接单 shows 暂无数据:", "暂无数据" in page.inner_text("body"))

        # ---------- Tab：已完成（应包含该订单） ----------
        page.click(".el-tabs__item:has-text('已完成')")
        page.wait_for_timeout(1000)
        body = page.inner_text("body")
        print("== tab 已完成 contains 订单号:", ORDER_NO in body)
        page.screenshot(path="d:/SchoolShop/.e2e/06-tab-done.png", full_page=True)

        browser.close()


if __name__ == "__main__":
    run()
