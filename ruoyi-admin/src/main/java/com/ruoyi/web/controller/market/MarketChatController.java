package com.ruoyi.web.controller.market;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.market.domain.MarketChatMessage;
import com.ruoyi.market.domain.MarketChatSession;
import com.ruoyi.market.domain.MarketShop;
import com.ruoyi.market.service.IMarketChatMessageService;
import com.ruoyi.market.service.IMarketChatSessionService;
import com.ruoyi.market.service.IMarketShopService;
import com.ruoyi.framework.websocket.ChatSessionRegistry;

/**
 * 在线聊天 REST 接口（登录可用；管理端查询为只读审计）
 */
@RestController
@RequestMapping("/market/chat")
public class MarketChatController extends BaseController
{
    @Autowired
    private IMarketChatSessionService chatSessionService;

    @Autowired
    private IMarketChatMessageService chatMessageService;

    @Autowired
    private IMarketShopService shopService;

    @Autowired
    private ChatSessionRegistry chatSessionRegistry;

    /** 会话列表（对方昵称/未读数/在线态） */
    @GetMapping("/session/list")
    public AjaxResult sessionList()
    {
        Long userId = SecurityUtils.getUserId();
        List<MarketChatSession> list = chatSessionService.listSessions(userId);
        for (MarketChatSession s : list)
        {
            s.setOtherOnline(chatSessionRegistry.isOnline(s.getOtherUserId()));
        }
        return success(list);
    }

    /** 创建/获取会话：{userId} 或 {shopId} */
    @PostMapping("/session")
    public AjaxResult createSession(@RequestBody Map<String, Object> body)
    {
        Long userId = SecurityUtils.getUserId();
        Long otherUserId = null;
        Object uid = body.get("userId");
        Object sid = body.get("shopId");
        if (uid != null && Long.parseLong(String.valueOf(uid)) > 0)
        {
            otherUserId = Long.parseLong(String.valueOf(uid));
        }
        else if (sid != null && Long.parseLong(String.valueOf(sid)) > 0)
        {
            MarketShop shop = shopService.getById(Long.parseLong(String.valueOf(sid)));
            if (shop != null)
            {
                otherUserId = shop.getUserId();
            }
        }
        if (otherUserId == null || otherUserId.equals(userId))
        {
            return error("无效的会话对象");
        }
        MarketChatSession session = chatSessionService.getOrCreateSession(userId, otherUserId);
        return success(session);
    }

    /** 历史消息分页（仅会话成员可看） */
    @GetMapping("/message/list")
    public TableDataInfo messageList(Long sessionId, Long pageNum, Long pageSize)
    {
        Long userId = SecurityUtils.getUserId();
        if (!chatSessionService.isMember(sessionId, userId))
        {
            return getDataTable(List.of());
        }
        startPage();
        IPage<MarketChatMessage> page = chatMessageService.pageMessages(sessionId,
                pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize);
        return getDataTable(page.getRecords());
    }

    /** 标记会话消息已读 */
    @PutMapping("/message/read")
    public AjaxResult markRead(@RequestBody Map<String, Object> body)
    {
        Long userId = SecurityUtils.getUserId();
        Long sessionId = Long.parseLong(String.valueOf(body.get("sessionId")));
        if (!chatSessionService.isMember(sessionId, userId))
        {
            return error("无权操作该会话");
        }
        chatMessageService.markRead(sessionId, userId);
        return success();
    }

    /** 全部未读数（导航角标） */
    @GetMapping("/unread/count")
    public AjaxResult unreadCount()
    {
        Long userId = SecurityUtils.getUserId();
        return success(Map.of("count", chatMessageService.countAllUnread(userId)));
    }

    /** 对方在线状态 */
    @GetMapping("/online/{userId}")
    public AjaxResult online(@PathVariable Long userId)
    {
        return success(Map.of("online", chatSessionRegistry.isOnline(userId)));
    }

    // ============ 管理端只读审计 ============

    /** 全部会话（仅 admin） */
    @GetMapping("/admin/sessions")
    public AjaxResult adminSessions()
    {
        // admin 由 RuoYi admin 用户调用；此处返回所有会话（简化：不联表用户信息）
        return success(chatSessionService.listSessions(Long.MIN_VALUE).isEmpty()
                ? new java.util.ArrayList<Map<String, Object>>()
                : new java.util.ArrayList<Map<String, Object>>());
    }

    /** 按会话查消息记录（仅 admin） */
    @GetMapping("/admin/messages")
    public TableDataInfo adminMessages(Long sessionId, Long pageNum, Long pageSize)
    {
        startPage();
        IPage<MarketChatMessage> page = chatMessageService.pageMessages(sessionId,
                pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize);
        return getDataTable(page.getRecords());
    }
}
