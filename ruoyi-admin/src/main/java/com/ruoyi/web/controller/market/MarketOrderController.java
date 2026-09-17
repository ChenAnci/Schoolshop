package com.ruoyi.web.controller.market;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.market.domain.MarketOrder;
import com.ruoyi.market.service.IMarketOrderService;

/**
 * 订单 操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/market/order")
public class MarketOrderController extends BaseController
{
    @Autowired
    private IMarketOrderService orderService;

    /**
     * 查询订单列表
     */
    @PreAuthorize("@ss.hasPermi('market:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(MarketOrder order)
    {
        startPage();
        List<MarketOrder> list = orderService.selectOrderList(order);
        return getDataTable(list);
    }

    /**
     * 获取订单详细信息
     */
    @PreAuthorize("@ss.hasPermi('market:order:query')")
    @GetMapping(value = "/{orderId}")
    public AjaxResult getInfo(@PathVariable Long orderId)
    {
        return success(orderService.getById(orderId));
    }

    /**
     * 新增订单
     */
    @PreAuthorize("@ss.hasPermi('market:order:add')")
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MarketOrder order)
    {
        order.setOrderNo(IdUtils.fastSimpleUUID());
        order.setCreateBy(getUsername());
        order.setCreateTime(DateUtils.getNowDate());
        return toAjax(orderService.save(order));
    }

    /**
     * 修改订单
     */
    @PreAuthorize("@ss.hasPermi('market:order:edit')")
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody MarketOrder order)
    {
        order.setUpdateBy(getUsername());
        order.setUpdateTime(DateUtils.getNowDate());
        return toAjax(orderService.updateById(order));
    }

    /**
     * 更新订单状态（含对应时间字段赋值）
     */
    @PreAuthorize("@ss.hasPermi('market:order:edit')")
    @Log(title = "订单状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult updateStatus(@RequestBody MarketOrder order)
    {
        order.setUpdateTime(DateUtils.getNowDate());
        switch (order.getStatus() == null ? "" : order.getStatus())
        {
            case "1":
                order.setTakeTime(DateUtils.getNowDate());
                break;
            case "2":
                order.setFinishTime(DateUtils.getNowDate());
                break;
            case "3":
                order.setCancelTime(DateUtils.getNowDate());
                break;
            default:
                break;
        }
        return toAjax(orderService.updateById(order));
    }

    /**
     * 删除订单
     */
    @PreAuthorize("@ss.hasPermi('market:order:remove')")
    @Log(title = "订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(orderService.removeByIds(Arrays.asList(orderIds)));
    }
}