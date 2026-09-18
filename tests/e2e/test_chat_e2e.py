# -*- coding: utf-8 -*-
"""E2E：用户端(ry@8083) 与 商家端(merchant@8082) 双浏览器互聊（后端 8088）"""
import json
import urllib.request

import pymysql
from playwright.sync_api import sync_playwright

BASE = "http://localhost:8088"


def clear_chat_tables():
    """测试环境清理：清空聊天表，保证会话列表干净（本地 E2E 专用）"""
    conn = pymysql.connect(host="127.0.0.1", port=3307, user="root", password="",
                           database="ruoyi_vue", charset="utf8mb4")
    try:
        cur = conn.cursor()
        cur.execute("DELETE FROM market_chat_message")
        cur.execute("DELETE FROM market_chat_session")
        conn.commit()
        cur.close()
    finally:
        conn.close()


def api_create_session(username, shop_id):
    """预置：用户与店铺商家创建会话（绕过 UI，聚焦聊天交互测试）"""
    body = json.dumps({"username": username, "password": "admin123"}).encode()
    req = urllib.request.Request(BASE + "/login", data=body,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=10) as resp:
        token = json.load(resp)["token"]
    req2 = urllib.request.Request(BASE + "/market/chat/session", method="POST",
                                  data=json.dumps({"shopId": shop_id}).encode(),
                                  headers={"Content-Type": "application/json",
                                           "Authorization": "Bearer " + token})
    with urllib.request.urlopen(req2, timeout=10) as resp:
        print("preset session:", json.load(resp).get("data", {}).get("sessionId"))


def login(page, url, username):
    page.goto(url)
    page.wait_for_load_state("networkidle")
    page.fill('input[placeholder="账号"]', username)
    page.fill('input[placeholder="密码"]', "admin123")
    page.click(".login-btn")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1500)


def main():
    clear_chat_tables()
    api_create_session("ry", 1)  # ry(2) <-> shop1 商家(100)

    CHROME = r"D:\SchoolShop\.pw-browsers\chromium-1234\chrome-win64\chrome.exe"
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, executable_path=CHROME)
        ctx_u = browser.new_context()
        ctx_m = browser.new_context()
        pu = ctx_u.new_page()
        pm = ctx_m.new_page()

        # 用户端：登录 ry（普通用户，留在 8083）
        login(pu, "http://localhost:8083/login", "ry")
        pu.goto("http://localhost:8083/chat")
        pu.wait_for_load_state("networkidle")
        pu.wait_for_timeout(2500)  # 等会话列表加载 + ws 连接

        # A(ry) 打开会话（ry 仅有与商家的一个会话）并发消息
        pu.click(".chat-item >> nth=0")
        pu.wait_for_timeout(800)
        pu.fill(".chat-input input", "E2E 你好商家")
        pu.click(".chat-input button")
        pu.wait_for_timeout(1500)

        # 商家端：8082 无登录页，会跳统一登录(8083)；登录 merchant 后跳回 8082
        login(pm, "http://localhost:8082/login", "merchant")
        pm.wait_for_timeout(2500)  # 等待跨端 token 跳转
        pm.goto("http://localhost:8082/chat")
        pm.wait_for_load_state("networkidle")
        pm.wait_for_timeout(2500)

        # B(merchant) 打开最新会话（ry 的消息已置顶）并应看到消息
        pm.click(".chat-item >> nth=0")
        pm.wait_for_timeout(800)
        pm.wait_for_selector("text=E2E 你好商家", timeout=15000)
        print("PASS: 用户->商家 实时送达")

        # B 回复
        pm.fill(".chat-input input", "E2E 你好买家")
        pm.click(".chat-input button")
        pu.wait_for_selector("text=E2E 你好买家", timeout=15000)
        print("PASS: 商家->用户 实时送达")

        browser.close()
        print("ALL E2E CHAT TESTS PASSED")


if __name__ == "__main__":
    main()
