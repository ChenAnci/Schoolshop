package com.ruoyi.market.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.market.domain.MarketCart;
import com.ruoyi.market.domain.MarketOrder;
import com.ruoyi.market.domain.MarketOrderCreateBo;
import com.ruoyi.market.domain.MarketOrderItem;
import com.ruoyi.market.domain.MarketProduct;
import com.ruoyi.market.mapper.MarketOrderMapper;
import com.ruoyi.market.service.IMarketCartService;
import com.ruoyi.market.service.IMarketOrderItemService;
import com.ruoyi.market.service.IMarketOrderService;
import com.ruoyi.market.service.IMarketProductService;

/**
 * 订单 服务层实现
 *
 * 承载下单的全部业务规则：归属校验 → 上下架与库存校验 → 按店铺拆单 → 落库 → 原子扣库存 → 清购物车。
 * 事务边界在本层（@Transactional 标注于 Service），Controller 不再出现业务判断与事务注解。
 *
 * @author ruoyi
 */
@Service
public class MarketOrderServiceImpl extends ServiceImpl<MarketOrderMapper, MarketOrder>
        implements IMarketOrderService
{
    /** 订单状态：待商家接单 */
    private static final String STATUS_WAIT_ACCEPT = "0";

    /** 订单状态：待自提 */
    private static final String STATUS_WAIT_TAKE = "1";

    /** 订单状态：已完成 */
    private static final String STATUS_FINISHED = "2";

    /** 订单状态：已取消 */
    private static final String STATUS_CANCELED = "3";

    /** 商品状态：上架 */
    private static final String PRODUCT_ON_SALE = "0";

    @Autowired
    private IMarketCartService cartService;

    @Autowired
    private IMarketProductService productService;

    @Autowired
    private IMarketOrderItemService orderItemService;

    @Override
    public List<MarketOrder> selectOrderList(MarketOrder order)
    {
        return baseMapper.selectOrderList(order);
    }

    /**
     * 结算下单：按店铺拆分购物车条目生成订单。
     *
     * 库存扣减使用原子 SQL（update ... where stock >= #{qty}），影响行数为 0 时抛业务异常回滚，
     * 避免「先查再改」在并发下造成的超卖。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> createOrder(Long userId, MarketOrderCreateBo bo)
    {
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        if (bo == null || bo.getCartIds() == null || bo.getCartIds().isEmpty())
        {
            throw new ServiceException("请选择要结算的商品");
        }

        // 1. 拉取属于当前用户的购物车条目（行级隔离，防止越权结算他人购物车）
        List<MarketCart> cartItems = cartService.lambdaQuery()
                .eq(MarketCart::getUserId, userId)
                .in(MarketCart::getCartId, bo.getCartIds())
                .list();
        if (cartItems == null || cartItems.isEmpty())
        {
            throw new ServiceException("购物车中没有可结算的商品");
        }

        // 2. 校验商品可用（存在 + 上架 + 库存充足），提前失败，避免产生脏订单
        Map<Long, MarketProduct> productMap = new HashMap<>();
        for (MarketCart cart : cartItems)
        {
            MarketProduct product = productService.getById(cart.getProductId());
            if (product == null)
            {
                throw new ServiceException("商品不存在");
            }
            if (!PRODUCT_ON_SALE.equals(product.getStatus()))
            {
                throw new ServiceException("商品「" + cart.getProductName() + "」已下架，请移除后重试");
            }
            if (product.getStock() == null || product.getStock() < cart.getQuantity())
            {
                throw new ServiceException("商品「" + cart.getProductName() + "」库存不足");
            }
            productMap.put(product.getProductId(), product);
        }

        // 3. 按店铺分组（同一店铺的商品合并为一个订单）
        //    注意：Java 9+ 的 Collectors.groupingBy 不允许 null key，故此处手工分组
        Map<Long, List<MarketCart>> byShop = new HashMap<>();
        for (MarketCart cart : cartItems)
        {
            Long shopId = productMap.get(cart.getProductId()).getShopId();
            if (shopId == null)
            {
                continue;
            }
            byShop.computeIfAbsent(shopId, k -> new ArrayList<>()).add(cart);
        }
        if (byShop.isEmpty())
        {
            throw new ServiceException("购物车中没有可结算的商品");
        }

        // 4. 逐店铺生成订单：订单 + 明细 + 原子扣库存 + 清购物车
        List<String> orderNos = new ArrayList<>();
        String prefix = DateUtils.dateTimeNow("yyyyMMddHHmmssSSS");
        Date now = DateUtils.getNowDate();
        int seq = 0;
        for (Map.Entry<Long, List<MarketCart>> entry : byShop.entrySet())
        {
            Long shopId = entry.getKey();
            List<MarketCart> shopCarts = entry.getValue();

            BigDecimal total = BigDecimal.ZERO;
            for (MarketCart cart : shopCarts)
            {
                total = total.add(cart.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            }

            MarketOrder order = buildOrder(userId, shopId, total, bo, prefix, seq++, now);
            this.save(order);

            for (MarketCart cart : shopCarts)
            {
                orderItemService.save(buildOrderItem(order, cart, now));
                // 原子扣减：库存不足时抛 ServiceException，事务整体回滚
                productService.deductStock(cart.getProductId(), cart.getQuantity());
                cartService.removeById(cart.getCartId());
            }
            orderNos.add(order.getOrderNo());
        }
        return orderNos;
    }

    /**
     * 取消订单（仅待接单状态可取消），并回滚库存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelOrder(Long userId, Long orderId)
    {
        MarketOrder order = getOwnOrder(userId, orderId);
        if (!STATUS_WAIT_ACCEPT.equals(order.getStatus()))
        {
            throw new ServiceException("当前状态不可取消");
        }

        // 回滚库存：订单取消后商品重新可售
        List<MarketOrderItem> items = orderItemService.lambdaQuery()
                .eq(MarketOrderItem::getOrderId, orderId)
                .list();
        if (items != null)
        {
            for (MarketOrderItem item : items)
            {
                if (item.getProductId() != null && item.getQuantity() != null)
                {
                    productService.restoreStock(item.getProductId(), item.getQuantity());
                }
            }
        }

        MarketOrder update = new MarketOrder();
        update.setOrderId(orderId);
        update.setStatus(STATUS_CANCELED);
        update.setCancelTime(DateUtils.getNowDate());
        update.setUpdateTime(DateUtils.getNowDate());
        return baseMapper.updateById(update);
    }

    /**
     * 确认收货（提货）：待自提 → 已完成。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmOrder(Long userId, Long orderId)
    {
        MarketOrder order = getOwnOrder(userId, orderId);
        if (!STATUS_WAIT_TAKE.equals(order.getStatus()))
        {
            throw new ServiceException("当前状态不可确认收货");
        }

        MarketOrder update = new MarketOrder();
        update.setOrderId(orderId);
        update.setStatus(STATUS_FINISHED);
        update.setFinishTime(order.getFinishTime() == null ? DateUtils.getNowDate() : order.getFinishTime());
        update.setUpdateTime(DateUtils.getNowDate());
        return baseMapper.updateById(update);
    }

    @Override
    public List<MarketOrder> listOwnOrders(Long userId, String status)
    {
        if (userId == null)
        {
            return new ArrayList<>();
        }
        if (status != null && !status.isEmpty())
        {
            return this.lambdaQuery()
                    .eq(MarketOrder::getUserId, userId)
                    .eq(MarketOrder::getStatus, status)
                    .orderByDesc(MarketOrder::getCreateTime)
                    .list();
        }
        return this.lambdaQuery()
                .eq(MarketOrder::getUserId, userId)
                .orderByDesc(MarketOrder::getCreateTime)
                .list();
    }

    @Override
    public MarketOrder getOwnOrderOrNull(Long userId, Long orderId)
    {
        return this.lambdaQuery()
                .eq(MarketOrder::getOrderId, orderId)
                .eq(MarketOrder::getUserId, userId)
                .one();
    }

    // ===================== 私有辅助方法 =====================

    /** 构造订单主记录（订单号：毫秒时间戳 + 序号，<= 32 位，满足 order_no 唯一索引长度） */
    private MarketOrder buildOrder(Long userId, Long shopId, BigDecimal total, MarketOrderCreateBo bo,
            String prefix, int seq, Date now)
    {
        MarketOrder order = new MarketOrder();
        order.setOrderNo(prefix + String.format("%02d", seq));
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setTotalAmount(total);
        order.setPayAmount(total);
        order.setStatus(STATUS_WAIT_ACCEPT);
        order.setReceiverName(bo.getReceiverName());
        order.setReceiverPhone(bo.getReceiverPhone());
        order.setReceiverAddress(bo.getReceiverAddress());
        order.setRemark(bo.getRemark());
        order.setCreateTime(now);
        return order;
    }

    /** 构造订单明细（商品名称/图片/单价均为下单时快照） */
    private MarketOrderItem buildOrderItem(MarketOrder order, MarketCart cart, Date now)
    {
        MarketOrderItem item = new MarketOrderItem();
        item.setOrderId(order.getOrderId());
        item.setOrderNo(order.getOrderNo());
        item.setProductId(cart.getProductId());
        item.setProductName(cart.getProductName());
        item.setProductImage(cart.getProductImage());
        item.setPrice(cart.getPrice());
        item.setQuantity(cart.getQuantity());
        item.setSubtotal(cart.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
        item.setCreateTime(now);
        return item;
    }

    /** 获取当前用户的订单，不存在时抛业务异常（归属校验，防止越权） */
    private MarketOrder getOwnOrder(Long userId, Long orderId)
    {
        MarketOrder order = getOwnOrderOrNull(userId, orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }
        return order;
    }
}
