package com.ruoyi.web.controller.market;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.market.domain.MarketOrder;
import com.ruoyi.market.service.IMarketOrderService;

/**
 * 管理端订单审查仲裁（管理员专属）
 *
 * 异常/争议订单处理：管理员将订单置为「审查中」(status=4)，进一步仲裁后落到终态
 * （强制已完成 status=2 / 强制已取消 status=3 / 恢复流转 status=0），并把处理结论写入审查位
 * （audit_flag 0未审查 1审查中 2已仲裁）与审查意见（audit_remark）。
 *
 * 权限：market:order:edit（管理员）。接口路径 /market/admin/order/**
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/market/admin/order")
public class MarketAdminOrderController extends BaseController
{
    @Autowired
    private IMarketOrderService orderService;

    /**
     * 进入审查中（status=4，audit_flag=1）
     */
    @PreAuthorize("@ss.hasPermi('market:order:edit')")
    @Log(title = "订单审查", businessType = BusinessType.UPDATE)
    @PutMapping("/{orderId}/start-audit")
    public AjaxResult startAudit(@PathVariable Long orderId, @RequestParam(required = false) String reason)
    {
        MarketOrder order = orderService.getById(orderId);
        if (order == null)
        {
            return error("订单不存在");
        }
        String status = order.getStatus() == null ? "" : order.getStatus();
        if ("4".equals(status) || "5".equals(status))
        {
            return error("该订单已进入审查流程，无需重复操作");
        }
        order.setStatus("4");
        order.setAuditFlag("1");
        order.setAuditRemark(StringUtils.trim(reason));
        order.setUpdateBy(getUsername());
        order.setUpdateTime(DateUtils.getNowDate());
        return toAjax(orderService.updateById(order));
    }

    /**
     * 仲裁：action 为 complete(强制已完成)/cancel(强制已取消)/restore(恢复流转)
     */
    @PreAuthorize("@ss.hasPermi('market:order:edit')")
    @Log(title = "订单仲裁", businessType = BusinessType.UPDATE)
    @PutMapping("/{orderId}/arbitrate")
    public AjaxResult arbitrate(@PathVariable Long orderId, @RequestBody Map<String, String> body)
    {
        MarketOrder order = orderService.getById(orderId);
        if (order == null)
        {
            return error("订单不存在");
        }
        if (!"4".equals(order.getStatus()))
        {
            return error("仅「审查中」的订单可进行仲裁");
        }
        String action = body.get("action");
        String remark = StringUtils.trim(body.get("remark"));
        if (StringUtils.isEmpty(action))
        {
            return error("请选择仲裁结论");
        }
        order.setAuditFlag("2");
        order.setAuditRemark(remark);
        order.setUpdateBy(getUsername());
        order.setUpdateTime(DateUtils.getNowDate());
        switch (action)
        {
            case "complete":
                order.setStatus("2");
                order.setFinishTime(DateUtils.getNowDate());
                break;
            case "cancel":
                order.setStatus("3");
                order.setCancelTime(DateUtils.getNowDate());
                break;
            case "restore":
                // 恢复流转：回到待商家接单，重新走流程
                order.setStatus("0");
                break;
            default:
                return error("仲裁结论不合法：complete/cancel/restore");
        }
        return toAjax(orderService.updateById(order));
    }
}