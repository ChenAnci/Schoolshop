package com.ruoyi.web.controller.market;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.market.domain.MarketCart;
import com.ruoyi.market.domain.MarketOrder;
import com.ruoyi.market.domain.MarketOrderCreateBo;
import com.ruoyi.market.domain.MarketOrderItem;
import com.ruoyi.market.domain.MarketProduct;
import com.ruoyi.market.domain.MarketReview;
import com.ruoyi.market.domain.MarketReviewSubmitBo;
import com.ruoyi.market.domain.MarketShop;
import com.ruoyi.market.service.IMarketCartService;
import com.ruoyi.market.service.IMarketOrderItemService;
import com.ruoyi.market.service.IMarketOrderService;
import com.ruoyi.market.service.IMarketProductService;
import com.ruoyi.market.service.IMarketReviewService;
import com.ruoyi.market.service.IMarketShopService;

/**
 * C 端用户 操作处理（当前登录用户作用域）
 *
 * 用户端（8083）使用，全部接口基于当前登录用户隔离（getUserId 行级隔离），
 * 不校验管理端权限点，普通用户登录后即可操作自己的购物车与订单。
 *
 * 分层约定：本类只负责「接收参数 → 调用 Service → 包装返回结果」，
 * 业务规则、事务边界、库存扣减均下沉至 Service 层（见 MarketOrderServiceImpl 等）。
 *
 * 接口路径：/market/user/**
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/market/user")
public class MarketUserController extends BaseController
{
    @Autowired
    private IMarketCartService cartService;

    @Autowired
    private IMarketProductService productService;

    @Autowired
    private IMarketShopService shopService;

    @Autowired
    private IMarketOrderService orderService;

    @Autowired
    private IMarketOrderItemService orderItemService;

    @Autowired
    private IMarketReviewService reviewService;

    // ===================== 购物车 =====================

    /**
     * 获取当前用户的购物车列表（含商品状态、店铺信息）
     */
    @GetMapping("/cart/list")
    public TableDataInfo cartList()
    {
        List<MarketCart> list = cartService.lambdaQuery()
                .eq(MarketCart::getUserId, getUserId())
                .orderByDesc(MarketCart::getCreateTime)
                .list();
        fillCartInfo(list);
        return getDataTable(list);
    }

    /**
     * 加入购物车（已存在则累加数量）
     */
    @PostMapping("/cart")
    public AjaxResult cartAdd(@RequestBody MarketCart cart)
    {
        if (cart == null)
        {
            return error("参数不合法");
        }
        return toAjax(cartService.addItem(getUserId(), cart.getProductId(), cart.getQuantity()));
    }

    /**
     * 修改购物车商品数量
     */
    @PutMapping("/cart")
    public AjaxResult cartEdit(@RequestBody MarketCart cart)
    {
        if (cart == null || cart.getCartId() == null)
        {
            return error("参数不合法");
        }
        return toAjax(cartService.updateQuantity(getUserId(), cart.getCartId(), cart.getQuantity()));
    }

    /**
     * 删除购物车条目
     */
    /**
     * 删除购物车条目（不属于当前用户的条目会被静默忽略）
     */
    @DeleteMapping("/cart/{cartIds}")
    public AjaxResult cartRemove(@PathVariable Long[] cartIds)
    {
        cartService.removeItems(getUserId(), cartIds);
        return success();
    }

    /**
     * 清空当前用户购物车
     */
    @DeleteMapping("/cart/clear")
    public AjaxResult cartClear()
    {
        cartService.clear(getUserId());
        return success();
    }

    // ===================== 订单 =====================

    /**
     * 获取当前用户的订单列表（可筛选状态）
     */
    @GetMapping("/order/list")
    public TableDataInfo orderList(@RequestParam(required = false) String status)
    {
        List<MarketOrder> list = orderService.listOwnOrders(getUserId(), status);
        fillShopName(list);
        return getDataTable(list);
    }

    /**
     * 获取当前用户订单详情（含明细，并标记每条明细是否已评价）
     */
    @GetMapping("/order/{orderId}")
    public AjaxResult orderInfo(@PathVariable Long orderId)
    {
        MarketOrder order = orderService.getOwnOrderOrNull(getUserId(), orderId);
        if (order == null)
        {
            return error("订单不存在");
        }
        fillShopName(Collections.singletonList(order));

        List<MarketOrderItem> items = orderItemService.lambdaQuery()
                .eq(MarketOrderItem::getOrderId, orderId)
                .list();
        // 标记每条明细是否已评价（避免重复评价）
        List<MarketReview> reviews = reviewService.lambdaQuery()
                .eq(MarketReview::getOrderId, orderId)
                .list();
        Set<Long> reviewedItemIds = new HashSet<>();
        if (reviews != null)
        {
            for (MarketReview r : reviews)
            {
                if (r.getOrderItemId() != null)
                {
                    reviewedItemIds.add(r.getOrderItemId());
                }
            }
        }
        if (items != null)
        {
            for (MarketOrderItem item : items)
            {
                item.setReviewed(reviewedItemIds.contains(item.getItemId()));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        return success(data);
    }

    /**
     * 下单：从购物车选中条目生成订单，按店铺拆分。
     *
     * 请求体：{"cartIds":[1,2], "receiverName":"","receiverPhone":"","receiverAddress":""}
     * 业务实现见 {@link com.ruoyi.market.service.IMarketOrderService#createOrder}，事务边界在 Service 层。
     */
    @PostMapping("/order")
    public AjaxResult orderCreate(@RequestBody MarketOrderCreateBo bo)
    {
        return success(orderService.createOrder(getUserId(), getUsername(), bo));
    }

    /**
     * 取消订单（仅待接单状态可取消，并回滚库存）
     */
    @PutMapping("/order/{orderId}/cancel")
    public AjaxResult orderCancel(@PathVariable Long orderId)
    {
        return toAjax(orderService.cancelOrder(getUserId(), orderId));
    }

    /**
     * 确认收货（提货）：待自提 → 已完成
     */
    @PutMapping("/order/{orderId}/confirm")
    public AjaxResult orderConfirm(@PathVariable Long orderId)
    {
        return toAjax(orderService.confirmOrder(getUserId(), orderId));
    }

    // ===================== 商品评价 =====================

    /**
     * 提交评价：对已完成订单的若干明细进行评价（每条明细仅可评价一次）。
     *
     * 请求体：{"reviews":[{"orderItemId":1,"rating":5,"content":"东西很好"}]}
     */
    @PostMapping("/review")
    public AjaxResult reviewCreate(@RequestBody MarketReviewSubmitBo bo)
    {
        int count = reviewService.submitReviews(getUserId(), bo);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return success(data);
    }

    // ===================== 视图补充（展示用，非业务规则） =====================

    /** 补充订单的店铺名称 */
    private void fillShopName(List<MarketOrder> list)
    {
        if (list == null)
        {
            return;
        }
        for (MarketOrder order : list)
        {
            if (order.getShopId() != null)
            {
                MarketShop shop = shopService.getById(order.getShopId());
                if (shop != null)
                {
                    order.setShopName(shop.getShopName());
                }
            }
        }
    }

    /** 填充购物车商品的当前状态与店铺信息 */
    private void fillCartInfo(List<MarketCart> list)
    {
        if (list == null)
        {
            return;
        }
        for (MarketCart cart : list)
        {
            MarketProduct product = productService.getById(cart.getProductId());
            if (product == null)
            {
                continue;
            }
            cart.setProductStatus(product.getStatus());
            cart.setShopId(product.getShopId());
            if (product.getShopId() != null)
            {
                MarketShop shop = shopService.getById(product.getShopId());
                if (shop != null)
                {
                    cart.setShopName(shop.getShopName());
                }
            }
        }
    }
}
