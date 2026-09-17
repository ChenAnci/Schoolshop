"""管理端订单审查仲裁 E2E（进入审查中→仲裁：强制取消/强制完成/恢复流转）"""
import subprocess
from playwright.sync_api import sync_playwright

ADM_BASE = "http://localhost:8081"
REDIS_CLI = r"d:\SchoolShop\toolchain\redis\redis-cli.exe"
MYSQL = r"d:\SchoolShop\toolchain\mysql-8.0.29-winx64\bin\mysql.exe"
ORDER_NO = "TMPADMOD202608"

RESET_SQL = f"UPDATE ruoyi_vue.market_orders SET status='1', audit_flag='0', audit_remark=NULL, cancel_time=NULL, finish_time=NULL WHERE order_no='{ORDER_NO}'"


def sql(q):
    subprocess.run([MYSQL, "-uroot", "-e", q], capture_output=True)


def row_status():
    r = subprocess.run([MYSQL, "-uroot", "-N", "-e",
                        f"SELECT CONCAT(status,'|',audit_flag,'|',IFNULL(audit_remark,'')) FROM ruoyi_vue.market_orders WHERE order_no='{ORDER_NO}'"],
                       capture_output=True, text=True)
    return r.stdout.strip()


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

        # 登录管理端
        page.goto(f"{ADM_BASE}/login")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)
        page.fill("input[placeholder='账号']", "admin")
        page.fill("input[placeholder='密码']", "admin123")
        page.fill("input[placeholder='验证码']", captcha_answer(captured.get("uuid", "")) if captured.get("uuid") else "")
        page.click("button:has-text('登 录')")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1000)
        assert "/dashboard" in page.url
        print("== logged in")

        def goto_order():
            page.goto(f"{ADM_BASE}/market/order")
            page.wait_for_load_state("networkidle")
            page.wait_for_timeout(1000)

        def wait_row():
            r = page.locator(f".el-table__body tr:has-text('{ORDER_NO}')").first
            r.wait_for(state="visible", timeout=8000)
            return r

        # ======== 场景1：进入审查 → 仲裁强制取消 ========
        goto_order()
        r = wait_row()
        # 该行目前 status=1待自提 且未审查 → 有「进入审查」按钮
        r.locator("button:has-text('进入审查')").first.click()
        page.wait_for_timeout(500)
        page.locator(".el-message-box__btns >> button:has-text('确认审查')").click()
        page.wait_for_timeout(1200)
        r = wait_row()
        print("== after start-audit DB state:", row_status(), "| 审查中 tag:", "审查中" in r.inner_text())
        # 仲裁强制取消
        r.locator("button:has-text('仲裁')").first.click()
        page.wait_for_timeout(600)
        page.locator(".el-radio:has-text('强制已取消')").click()
        page.locator("textarea").fill("用户投诉未收到货，商家逾期，仲裁退款")
        page.click("button:has-text('提交仲裁')")
        page.wait_for_timeout(1200)
        r = wait_row()
        print("== after arbitrate-cancel DB state:", row_status(), "| 已取消 tag:", "已取消" in r.inner_text(), "| 已仲裁 tag:", "已仲裁" in r.inner_text())
        assert row_status().startswith("3|2")
        page.screenshot(path="d:/SchoolShop/.e2e/30-arbitrate-cancel.png")

        # ======== 场景2：复位 → 进入审查 → 仲裁强制完成 ========
        sql(RESET_SQL)
        goto_order()
        r = wait_row()
        r.locator("button:has-text('进入审查')").first.click()
        page.wait_for_timeout(500)
        page.locator(".el-message-box__btns >> button:has-text('确认审查')").click()
        page.wait_for_timeout(1200)
        r = wait_row()
        r.locator("button:has-text('仲裁')").first.click()
        page.wait_for_timeout(600)
        page.locator(".el-radio:has-text('强制已完成')").click()
        page.locator("textarea").fill("双方协商一致，按完成处理")
        page.click("button:has-text('提交仲裁')")
        page.wait_for_timeout(1200)
        r = wait_row()
        print("== after arbitrate-complete DB state:", row_status())
        assert row_status().startswith("2|2")
        print("== complete ok")

        # ======== 场景3：复位 → 进入审查 → 仲裁恢复流转 ========
        sql(RESET_SQL)
        goto_order()
        r = wait_row()
        r.locator("button:has-text('进入审查')").first.click()
        page.wait_for_timeout(500)
        page.locator(".el-message-box__btns >> button:has-text('确认审查')").click()
        page.wait_for_timeout(1200)
        r = wait_row()
        r.locator("button:has-text('仲裁')").first.click()
        page.wait_for_timeout(600)
        page.locator(".el-radio:has-text('恢复流转')").click()
        page.locator("textarea").fill("误判，恢复待商家接单重新流转")
        page.click("button:has-text('提交仲裁')")
        page.wait_for_timeout(1200)
        r = wait_row()
        print("== after arbitrate-restore DB state:", row_status())
        assert row_status().startswith("0|2")
        print("== restore ok")

        # 清理测试订单
        sql(f"DELETE FROM ruoyi_vue.market_orders WHERE order_no='{ORDER_NO}'")
        print("== cleanup done")
        browser.close()


if __name__ == "__main__":
    run()
    print("ALL ASSERTIONS PASSED")