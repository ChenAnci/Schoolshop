package com.ruoyi.web.controller.market;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.market.domain.MarketOrder;
import com.ruoyi.market.domain.MarketOrderItem;
import com.ruoyi.market.domain.MarketShop;
import com.ruoyi.market.service.IMarketOrderItemService;
import com.ruoyi.market.service.IMarketOrderService;
import com.ruoyi.market.service.IMarketShopService;

/**
 * 商家端订单处理（商家作用域）
 *
 * 商家端（8082）使用，全部接口基于当前登录商家所属店铺隔离（getUserId → shop_id），
 * 仅能操作自己店铺的订单，防止越权操作其他商家的订单。
 *
 * 状态流转（开发文档 §7）：
 *   0待商家接单 ──接单──> 1待自提 ──确认取货──> 2已完成
 *   0待商家接单 ──拒单/取消──> 3已取消
 *
 * 接口路径：/market/shop/order/**
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/market/shop/order")
public class MarketShopOrderController extends BaseController
{
    @Autowired
    private IMarketShopService shopService;

    @Autowired
    private IMarketOrderService orderService;

    @Autowired
    private IMarketOrderItemService orderItemService;

    /** 获取当前商家自己的店铺，未开通返回 null */
    private MarketShop getMyShop()
    {
        return shopService.lambdaQuery()
                .eq(MarketShop::getUserId, getUserId())
                .one();
    }

    /** 校验订单归属当前商家店铺，不匹配返回 null */
    private MarketOrder getOwnOrder(Long orderId, Long shopId)
    {
        return orderService.lambdaQuery()
                .eq(MarketOrder::getOrderId, orderId)
                .eq(MarketOrder::getShopId, shopId)
                .one();
    }

    /**
     * 获取当前商家店铺的订单列表（可按状态筛选，含买家昵称）
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String status)
    {
        MarketShop shop = getMyShop();
        if (shop == null)
        {
            return getDataTable(List.of());
        }
        MarketOrder query = new MarketOrder();
        query.setShopId(shop.getShopId());
        query.setStatus(status);
        startPage();
        List<MarketOrder> list = orderService.selectOrderList(query);
        return getDataTable(list);
    }

    /**
     * 获取订单详情（含明细）
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @GetMapping("/{orderId}")
    public AjaxResult info(@PathVariable Long orderId)
    {
        MarketShop shop = getMyShop();
        if (shop == null)
        {
            return error("尚未开通店铺");
        }
        MarketOrder order = getOwnOrder(orderId, shop.getShopId());
        if (order == null)
        {
            return error("订单不存在");
        }
        List<MarketOrderItem> items = orderItemService.lambdaQuery()
                .eq(MarketOrderItem::getOrderId, orderId)
                .list();
        return success(Map.of("order", order, "items", items));
    }

    /**
     * 接单：0待商家接单 → 1待自提
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @PutMapping("/{orderId}/accept")
    public AjaxResult accept(@PathVariable Long orderId)
    {
        MarketShop shop = getMyShop();
        if (shop == null)
        {
            return error("尚未开通店铺");
        }
        MarketOrder order = getOwnOrder(orderId, shop.getShopId());
        if (order == null)
        {
            return error("订单不存在");
        }
        if (!"0".equals(order.getStatus()))
        {
            return error("当前状态不可接单");
        }
        order.setStatus("1");
        order.setUpdateBy(getUsername());
        order.setUpdateTime(DateUtils.getNowDate());
        return toAjax(orderService.updateById(order));
    }

    /**
     * 拒单/取消：0待商家接单 → 3已取消
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @PutMapping("/{orderId}/cancel")
    public AjaxResult cancel(@PathVariable Long orderId)
    {
        MarketShop shop = getMyShop();
        if (shop == null)
        {
            return error("尚未开通店铺");
        }
        MarketOrder order = getOwnOrder(orderId, shop.getShopId());
        if (order == null)
        {
            return error("订单不存在");
        }
        if (!"0".equals(order.getStatus()))
        {
            return error("当前状态不可取消");
        }
        order.setStatus("3");
        order.setCancelTime(DateUtils.getNowDate());
        order.setUpdateBy(getUsername());
        order.setUpdateTime(DateUtils.getNowDate());
        return toAjax(orderService.updateById(order));
    }
}
