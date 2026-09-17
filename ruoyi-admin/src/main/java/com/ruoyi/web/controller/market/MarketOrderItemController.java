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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.market.domain.MarketOrderItem;
import com.ruoyi.market.service.IMarketOrderItemService;

/**
 * 订单明细 操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/market/orderItem")
public class MarketOrderItemController extends BaseController
{
    @Autowired
    private IMarketOrderItemService orderItemService;

    /**
     * 查询订单明细列表
     */
    @PreAuthorize("@ss.hasPermi('market:orderItem:list')")
    @GetMapping("/list")
    public TableDataInfo list(MarketOrderItem orderItem)
    {
        startPage();
        List<MarketOrderItem> list = orderItemService.selectOrderItemList(orderItem);
        return getDataTable(list);
    }

    /**
     * 获取订单明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('market:orderItem:query')")
    @GetMapping(value = "/{itemId}")
    public AjaxResult getInfo(@PathVariable Long itemId)
    {
        return success(orderItemService.getById(itemId));
    }

    /**
     * 新增订单明细
     */
    @PreAuthorize("@ss.hasPermi('market:orderItem:add')")
    @Log(title = "订单明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MarketOrderItem orderItem)
    {
        orderItem.setCreateTime(DateUtils.getNowDate());
        return toAjax(orderItemService.save(orderItem));
    }

    /**
     * 删除订单明细
     */
    @PreAuthorize("@ss.hasPermi('market:orderItem:remove')")
    @Log(title = "订单明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{itemIds}")
    public AjaxResult remove(@PathVariable Long[] itemIds)
    {
        return toAjax(orderItemService.removeByIds(Arrays.asList(itemIds)));
    }
}