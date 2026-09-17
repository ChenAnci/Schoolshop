# -*- coding: utf-8 -*-
"""聊天 REST 接口冒烟（后端 8088）：admin(1) 视角"""
import json
import urllib.request

BASE = "http://localhost:8088"


def call(method, path, token=None, body=None):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode() if body else None
    with urllib.request.urlopen(req, data=data, timeout=10) as resp:
        return json.load(resp)


def main():
    login = call("POST", "/login", body={"username": "admin", "password": "admin123"})
    tk = login["token"]

    # 1) 会话列表
    r = call("GET", "/market/chat/session/list", tk)
    print("session/list code=%s rows=%s" % (r.get("code"), len(r.get("data", []))))
    for s in r.get("data", []):
        print("  session=%s other=%s nick=%s unread=%s online=%s last=%s"
              % (s["sessionId"], s["otherUserId"], s["otherNickName"],
                 s["unreadCount"], s["otherOnline"], s["lastMessage"]))

    # 2) 未读数
    r = call("GET", "/market/chat/unread/count", tk)
    print("unread/count =", r.get("data"))

    # 3) 历史消息
    r = call("GET", "/market/chat/message/list?sessionId=2&pageNum=1&pageSize=10", tk)
    print("message/list total=%s rows=%s" % (r.get("total"), len(r.get("rows", []))))

    # 4) 在线状态（merchant=100）
    r = call("GET", "/market/chat/online/100", tk)
    print("online/100 =", r.get("data"))

    # 5) 店铺创建会话（shopId=1 -> 商家 userId=100）
    r = call("POST", "/market/chat/session", tk, body={"shopId": 1})
    print("create by shopId code=%s session=%s" % (r.get("code"), r.get("data", {}).get("sessionId")))

    print("ALL REST SMOKE PASSED")


if __name__ == "__main__":
    main()
