package com.ruoyi.framework.websocket;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;

/**
 * WebSocket 握手鉴权：从 query token 解析登录用户，写入 session attributes。
 * 验证失败直接拒绝握手。
 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor
{
    private static final Logger log = LoggerFactory.getLogger(ChatHandshakeInterceptor.class);

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_NICK = "nickName";

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes)
    {
        String query = request.getURI().getQuery();
        String token = null;
        if (StringUtils.isNotEmpty(query))
        {
            for (String pair : query.split("&"))
            {
                if (pair.startsWith("token="))
                {
                    token = pair.substring("token=".length());
                    break;
                }
            }
        }
        if (StringUtils.isEmpty(token))
        {
            log.warn("WebSocket 握手缺少 token");
            return false;
        }
        LoginUser loginUser = tokenService.getLoginUserByToken(token);
        if (loginUser == null || loginUser.getUser() == null)
        {
            log.warn("WebSocket 握手 token 无效");
            return false;
        }
        attributes.put(ATTR_USER_ID, loginUser.getUserId());
        attributes.put(ATTR_NICK, loginUser.getUser().getNickName());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception)
    {
    }
}
