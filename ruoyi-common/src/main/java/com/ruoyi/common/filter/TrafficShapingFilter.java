package com.ruoyi.common.filter;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流量整形过滤器 —— 高并发保护
 *
 * 按配置比例随机拒绝请求，过滤掉 90% 的请求，只放行配置比例的请求到后端，
 * 保护数据库和业务逻辑不被亿级并发压垮。白名单路径（登录/刷新/健康检查）永远放行。
 *
 * @author system
 */
public class TrafficShapingFilter implements Filter
{
    private static final Logger log = LoggerFactory.getLogger(TrafficShapingFilter.class);

    private final AtomicLong passedRequests = new AtomicLong(0);
    private final AtomicLong rejectedRequests = new AtomicLong(0);

    private boolean enabled;
    private double passRatio;
    private String[] whitelist;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        enabled = Boolean.parseBoolean(filterConfig.getInitParameter("enabled"));
        passRatio = Double.parseDouble(filterConfig.getInitParameter("passRatio"));
        String whitelistStr = filterConfig.getInitParameter("whitelist");
        whitelist = whitelistStr != null && !whitelistStr.isEmpty()
                ? whitelistStr.split(",")
                : new String[0];

        log.info("TrafficShapingFilter initialized: enabled={}, passRatio={}, whitelist={}",
                enabled, passRatio, whitelistStr);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        if (!enabled)
        {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();

        // 白名单路径永远放行
        for (String prefix : whitelist)
        {
            if (path.startsWith(prefix.trim()))
            {
                passedRequests.incrementAndGet();
                chain.doFilter(request, response);
                return;
            }
        }

        // 随机按比例放行
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() < passRatio)
        {
            passedRequests.incrementAndGet();
            chain.doFilter(request, response);
        }
        else
        {
            rejectedRequests.incrementAndGet();
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":503,\"msg\":\"系统繁忙，请稍后重试\",\"totalPassed\":"
                    + passedRequests.get() + ",\"totalRejected\":" + rejectedRequests.get() + "}");

            // 每 1000 个被拒绝的打一次日志，避免刷屏
            long rejected = rejectedRequests.get();
            if (rejected % 1000 == 0)
            {
                log.info("TrafficShaping: passed={}, rejected={}, passRatio={}",
                        passedRequests.get(), rejected, passRatio);
            }
        }
    }

    @Override
    public void destroy()
    {
        log.info("TrafficShapingFilter destroyed: totalPassed={}, totalRejected={}",
                passedRequests.get(), rejectedRequests.get());
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public double getPassRatio()
    {
        return passRatio;
    }

    public void setPassRatio(double passRatio)
    {
        this.passRatio = passRatio;
    }

    public String[] getWhitelist()
    {
        return whitelist;
    }

    public void setWhitelist(String[] whitelist)
    {
        this.whitelist = whitelist;
    }
}
