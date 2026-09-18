package com.ruoyi.web.controller.market;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
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
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.market.domain.MarketCart;
import com.ruoyi.market.domain.MarketOrder;
import com.ruoyi.market.domain.MarketOrderItem;
import com.ruoyi.market.domain.MarketProduct;
import com.ruoyi.market.domain.MarketReview;
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
        if (cart.getProductId() == null)
        {
            return error("商品不能为空");
        }
        MarketProduct product = productService.getById(cart.getProductId());
        if (product == null)
        {
            return error("商品不存在");
        }
        // 库存校验
        int qty = cart.getQuantity() == null ? 1 : cart.getQuantity();
        if (qty < 1)
        {
            return error("数量不合法");
        }
        if (product.getStock() == null || product.getStock() < qty)
        {
            return error("库存不足");
        }
        Date now = DateUtils.getNowDate();
        // 已存在则累加
        MarketCart exist = cartService.lambdaQuery()
                .eq(MarketCart::getUserId, getUserId())
                .eq(MarketCart::getProductId, cart.getProductId())
                .one();
        if (exist != null)
        {
            exist.setQuantity((exist.getQuantity() == null ? 0 : exist.getQuantity()) + qty);
            if (exist.getQuantity() > product.getStock())
            {
                return error("库存不足");
            }
            exist.setUpdateTime(now);
            return toAjax(cartService.updateById(exist));
        }
        MarketCart add = new MarketCart();
        add.setUserId(getUserId());
        add.setProductId(product.getProductId());
        add.setProductName(product.getProductName());
        add.setProductImage(product.getProductImage());
        add.setPrice(product.getPrice());
        add.setQuantity(qty);
        add.setChecked("1");
        add.setCreateTime(now);
        add.setUpdateTime(now);
        return toAjax(cartService.save(add));
    }

    /**
     * 修改购物车商品数量
     */
    @PutMapping("/cart")
    public AjaxResult cartEdit(@RequestBody MarketCart cart)
    {
        if (cart.getCartId() == null)
        {
            return error("参数不合法");
        }
        MarketCart own = getOwnCart(cart.getCartId());
        if (own == null)
        {
            return error("购物车条目不存在");
        }
        int qty = cart.getQuantity() == null || cart.getQuantity() < 1 ? 1 : cart.getQuantity();
        MarketProduct product = productService.getById(own.getProductId());
        if (product != null && product.getStock() != null && qty > product.getStock())
        {
            return error("库存不足");
        }
        own.setQuantity(qty);
        own.setUpdateTime(DateUtils.getNowDate());
        return toAjax(cartService.updateById(own));
    }

    /**
     * 删除购物车条目
     */
    @DeleteMapping("/cart/{cartIds}")
    public AjaxResult cartRemove(@PathVariable Long[] cartIds)
    {
        List<Long> owned = new ArrayList<>();
        for (Long id : cartIds)
        {
            MarketCart own = getOwnCart(id);
            if (own != null)
            {
                owned.add(id);
            }
        }
        if (owned.isEmpty())
        {
            return success();
        }
        return toAjax(cartService.removeByIds(owned));
    }

    /**
     * 清空当前用户购物车
     */
    @DeleteMapping("/cart/clear")
    public AjaxResult cartClear()
    {
        cartService.lambdaUpdate().eq(MarketCart::getUserId, getUserId()).remove();
        return success();
    }

    // ===================== 订单 =====================

    /**
     * 获取当前用户的订单列表（可筛选状态）
     */
    @GetMapping("/order/list")
    public TableDataInfo orderList(@RequestParam(required = false) String status)
    {
        Long userId = getUserId();
        List<MarketOrder> list;
        if (status != null && !status.isEmpty())
        {
            list = orderService.lambdaQuery()
                    .eq(MarketOrder::getUserId, userId)
                    .eq(MarketOrder::getStatus, status)
                    .orderByDesc(MarketOrder::getCreateTime)
                    .list();
        }
        else
        {
            list = orderService.lambdaQuery()
                    .eq(MarketOrder::getUserId, userId)
                    .orderByDesc(MarketOrder::getCreateTime)
                    .list();
        }
        // 补充店铺名
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
        return getDataTable(list);
    }

    /**
     * 获取当前用户订单详情（含明细）
     */
    @GetMapping("/order/{orderId}")
    public AjaxResult orderInfo(@PathVariable Long orderId)
    {
        MarketOrder order = getOwnOrder(orderId);
        if (order == null)
        {
            return error("订单不存在");
        }
        if (order.getShopId() != null)
        {
            MarketShop shop = shopService.getById(order.getShopId());
            if (shop != null)
            {
                order.setShopName(shop.getShopName());
            }
        }
        List<MarketOrderItem> items = orderItemService.lambdaQuery()
                .eq(MarketOrderItem::getOrderId, orderId)
                .list();
        // 标记每条明细是否已评价（避免重复评价）
        List<MarketReview> reviews = reviewService.lambdaQuery()
                .eq(MarketReview::getOrderId, orderId)
                .list();
        Set<Long> reviewedItemIds = new HashSet<>();
        for (MarketReview r : reviews)
        {
            if (r.getOrderItemId() != null)
            {
                reviewedItemIds.add(r.getOrderItemId());
            }
        }
        for (MarketOrderItem item : items)
        {
            item.setReviewed(reviewedItemIds.contains(item.getItemId()));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        return success(data);
    }

    /**
     * 下单：从购物车选中条目生成订单。
     * 请求体：{"cartIds":[1,2], "receiverName":"","receiverPhone":"","receiverAddress":""}
     * 按店铺拆分订单，事务内创建订单+明细、扣库存、增销量、清购物车条目。
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/order")
    public AjaxResult orderCreate(@RequestBody Map<String, Object> body)
    {
        List<Long> cartIds = toLongList(body.get("cartIds"));
        if (cartIds == null || cartIds.isEmpty())
        {
            return error("请选择要结算的商品");
        }
        // 拉取当前用户且属于所选 cartId 的条目（防止越权访问他人购物车）
        List<MarketCart> cartItems = cartService.lambdaQuery()
                .eq(MarketCart::getUserId, getUserId())
                .in(MarketCart::getCartId, cartIds)
                .list();
        if (cartItems == null || cartItems.isEmpty())
        {
            return error("购物车中没有可结算的商品");
        }
        // 按店铺分组
        Map<Long, List<MarketCart>> byShop = cartItems.stream()
                .collect(Collectors.groupingBy(cart -> {
                    MarketProduct p = productService.getById(cart.getProductId());
                    return p == null ? null : p.getShopId();
                }));
        Set<Long> allShopIds = new HashSet<>();
        for (Map.Entry<Long, List<MarketCart>> entry : byShop.entrySet())
        {
            allShopIds.add(entry.getKey());
        }
        // 校验库存可用
        for (MarketCart cart : cartItems)
        {
            MarketProduct product = productService.getById(cart.getProductId());
            if (product == null || !"0".equals(product.getStatus()))
            {
                return error("商品「" + cart.getProductName() + "」已下架，请移除后重试");
            }
            if (product.getStock() == null || product.getStock() < cart.getQuantity())
            {
                return error("商品「" + cart.getProductName() + "」库存不足");
            }
        }
        List<String> orderNos = new ArrayList<>();
        // 订单号：毫秒时间戳 + 序号（<= 32 位，满足 order_no 唯一索引长度）
        String prefix = DateUtils.dateTimeNow("yyyyMMddHHmmssSSS");
        Date now = DateUtils.getNowDate();
        int seq = 0;
        for (Long shopId : allShopIds)
        {
            List<MarketCart> shopCarts = byShop.get(shopId);
            if (shopCarts == null || shopCarts.isEmpty())
            {
                continue;
            }
            String orderNo = prefix + String.format("%02d", seq++);
            BigDecimal total = BigDecimal.ZERO;
            for (MarketCart cart : shopCarts)
            {
                total = total.add(cart.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            }
            MarketOrder order = new MarketOrder();
            order.setOrderNo(orderNo);
            order.setUserId(getUserId());
            order.setShopId(shopId);
            order.setTotalAmount(total);
            order.setPayAmount(total);
            order.setStatus("0");
            order.setReceiverName(str(body.get("receiverName")));
            order.setReceiverPhone(str(body.get("receiverPhone")));
            order.setReceiverAddress(str(body.get("receiverAddress")));
            order.setCreateBy(getUsername());
            order.setCreateTime(now);
            order.setRemark(str(body.get("remark")));
            orderService.save(order);
            // 明细 + 扣库存 + 增销量
            for (MarketCart cart : shopCarts)
            {
                MarketOrderItem item = new MarketOrderItem();
                item.setOrderId(order.getOrderId());
                item.setOrderNo(orderNo);
                item.setProductId(cart.getProductId());
                item.setProductName(cart.getProductName());
                item.setProductImage(cart.getProductImage());
                item.setPrice(cart.getPrice());
                item.setQuantity(cart.getQuantity());
                item.setSubtotal(cart.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
                item.setCreateTime(now);
                orderItemService.save(item);
                // 扣库存增销量
                MarketProduct product = productService.getById(cart.getProductId());
                product.setStock(product.getStock() - cart.getQuantity());
                product.setSales(product.getSales() == null ? cart.getQuantity() : product.getSales() + cart.getQuantity());
                productService.updateById(product);
                // 删除已结算购物车条目
                cartService.removeById(cart.getCartId());
            }
            orderNos.add(orderNo);
        }
        return success(orderNos);
    }

    /**
     * 取消订单（仅待接单状态可取消）
     */
    @PutMapping("/order/{orderId}/cancel")
    public AjaxResult orderCancel(@PathVariable Long orderId)
    {
        MarketOrder order = getOwnOrder(orderId);
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
        order.setUpdateTime(DateUtils.getNowDate());
        return toAjax(orderService.updateById(order));
    }

    /**
     * 确认收货（提货）：1待自提 → 2已完成，完成订单闭环
     */
    @PutMapping("/order/{orderId}/confirm")
    public AjaxResult orderConfirm(@PathVariable Long orderId)
    {
        MarketOrder order = getOwnOrder(orderId);
        if (order == null)
        {
            return error("订单不存在");
        }
        // 仅待自提/待提货可确认，超出即不可再操作
        if ("1".equals(order.getStatus()))
        {
            if (order.getFinishTime() == null)
            {
                order.setFinishTime(DateUtils.getNowDate());
            }
            order.setStatus("2");
            order.setUpdateTime(DateUtils.getNowDate());
            return toAjax(orderService.updateById(order));
        }
        return error("当前状态不可确认收货");
    }

    // ===================== 商品评价 =====================

    /**
     * 提交评价：对已完成订单的若干明细进行评价（每条明细仅可评价一次）。
     * 请求体：{"reviews":[{"orderItemId":1,"rating":5,"content":"东西很好"}]}
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/review")
    public AjaxResult reviewCreate(@RequestBody Map<String, Object> body)
    {
        List<Map<String, Object>> reviewList = toMapList(body.get("reviews"));
        if (reviewList == null || reviewList.isEmpty())
        {
            return error("请选择要评价的商品");
        }
        Date now = DateUtils.getNowDate();
        int created = 0;
        for (Map<String, Object> item : reviewList)
        {
            Long itemId = toLong(item.get("orderItemId"));
            if (itemId == null)
            {
                continue;
            }
            MarketOrderItem orderItem = orderItemService.getById(itemId);
            if (orderItem == null)
            {
                return error("订单明细不存在");
            }
            MarketOrder order = getOwnOrder(orderItem.getOrderId());
            if (order == null)
            {
                return error("无权评价该订单");
            }
            // 仅已完成订单可评价
            if (!"2".equals(order.getStatus()))
            {
                return error("仅已完成的订单可评价");
            }
            // 去重：同一明细不可重复评价
            Long existing = reviewService.lambdaQuery()
                    .eq(MarketReview::getOrderItemId, itemId)
                    .count();
            if (existing != null && existing > 0)
            {
                continue;
            }
            MarketReview review = new MarketReview();
            review.setOrderId(order.getOrderId());
            review.setOrderItemId(itemId);
            review.setUserId(order.getUserId());
            review.setProductId(orderItem.getProductId());
            review.setShopId(order.getShopId());
            Integer rating = toInt(item.get("rating"));
            review.setRating(rating == null || rating < 1 ? 5 : Math.min(rating, 5));
            review.setContent(str(item.get("content")));
            review.setCreateTime(now);
            reviewService.save(review);
            created++;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("count", created);
        return success(data);
    }

    // ===================== 工具方法 =====================

    /** 校验购物车条目归属当前用户 */
    private MarketCart getOwnCart(Long cartId)
    {
        return cartService.lambdaQuery()
                .eq(MarketCart::getCartId, cartId)
                .eq(MarketCart::getUserId, getUserId())
                .one();
    }

    /** 校验订单归属当前用户 */
    private MarketOrder getOwnOrder(Long orderId)
    {
        return orderService.lambdaQuery()
                .eq(MarketOrder::getOrderId, orderId)
                .eq(MarketOrder::getUserId, getUserId())
                .one();
    }

    /** 填充购物车商品的店铺信息 */
    private void fillCartInfo(List<MarketCart> list)
    {
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

    private List<Long> toLongList(Object obj)
    {
        if (!(obj instanceof List))
        {
            return null;
        }
        List<Long> result = new ArrayList<>();
        for (Object o : (List<?>) obj)
        {
            if (o != null)
            {
                result.add(Long.valueOf(o.toString()));
            }
        }
        return result;
    }

    private String str(Object obj)
    {
        return obj == null ? null : obj.toString();
    }

    /** 将 Object 解析为 Map 列表（评价请求体解析） */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMapList(Object obj)
    {
        if (!(obj instanceof List))
        {
            return null;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object o : (List<?>) obj)
        {
            if (o instanceof Map)
            {
                result.add((Map<String, Object>) o);
            }
        }
        return result;
    }

    private Long toLong(Object obj)
    {
        return obj == null ? null : Long.valueOf(obj.toString());
    }

    private Integer toInt(Object obj)
    {
        return obj == null ? null : Integer.valueOf(obj.toString());
    }

    private Set<Long> toSet(Long[] arr)
    {
        return arr == null ? new HashSet<>() : new HashSet<>(Arrays.asList(arr));
    }
}