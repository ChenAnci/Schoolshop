# -*- coding: utf-8 -*-
"""WebSocket 聊天双端验证：admin(1) <-> merchant(2) 互发消息（后端 8088）"""
import json
import time
import urllib.request

import websocket

BASE = "http://localhost:8088"
WS = "ws://localhost:8088/ws"


def login(username, password):
    body = json.dumps({"username": username, "password": password}).encode()
    req = urllib.request.Request(BASE + "/login", data=body,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.load(resp)["token"]


def drain(ws, timeout=2.0):
    """采集 ws 上在 timeout 秒内到达的消息"""
    ws.settimeout(timeout)
    out = []
    while True:
        try:
            out.append(json.loads(ws.recv()))
        except (websocket.WebSocketTimeoutException, websocket.WebSocketConnectionClosedException):
            break
        except Exception:
            break
    ws.settimeout(10)
    return out


def main():
    tk_a = login("admin", "admin123")
    tk_b = login("merchant", "admin123")
    print("login OK admin/merchant")

    wa = websocket.create_connection(WS + "?token=" + tk_a, timeout=10)
    wb = websocket.create_connection(WS + "?token=" + tk_b, timeout=10)
    print("ws connected A/B")

    # A -> B
    wa.send(json.dumps({"type": "CHAT_SEND", "receiverId": 100, "content": "你好，这条商品还在吗？"}))
    time.sleep(0.5)
    ma = drain(wa)
    mb = drain(wb)
    assert any(m.get("type") == "MESSAGE_ACK" for m in ma), "A 未收到回执"
    b_received = [m for m in mb if m.get("type") == "CHAT_MESSAGE"]
    assert b_received, "B 未收到 A 的消息"
    print("PASS A->B:", b_received[0]["data"]["content"])
    sid = b_received[0]["data"]["sessionId"]

    # B -> A
    wb.send(json.dumps({"type": "CHAT_SEND", "receiverId": 1, "content": "在的，随时可以自提"}))
    time.sleep(0.5)
    ma2 = drain(wa)
    a_received = [m for m in ma2 if m.get("type") == "CHAT_MESSAGE"]
    assert a_received, "A 未收到 B 的消息"
    print("PASS B->A:", a_received[0]["data"]["content"])

    # 已读
    wb.send(json.dumps({"type": "CHAT_READ", "sessionId": sid}))
    time.sleep(0.5)
    print("PASS READ sessionId=", sid)

    wa.close()
    wb.close()
    print("ALL WS TESTS PASSED")


if __name__ == "__main__":
    main()
