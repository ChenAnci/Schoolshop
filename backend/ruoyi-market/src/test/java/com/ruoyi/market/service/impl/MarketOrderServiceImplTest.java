package com.ruoyi.market.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.market.domain.MarketCart;
import com.ruoyi.market.domain.MarketOrder;
import com.ruoyi.market.domain.MarketOrderCreateBo;
import com.ruoyi.market.domain.MarketOrderItem;
import com.ruoyi.market.domain.MarketProduct;
import com.ruoyi.market.mapper.MarketOrderMapper;
import com.ruoyi.market.service.IMarketCartService;
import com.ruoyi.market.service.IMarketOrderItemService;
import com.ruoyi.market.service.IMarketProductService;

/**
 * 订单服务单元测试
 *
 * 纯单元测试：mock Mapper 与依赖 Service，不启动 Spring 上下文、不连接数据库。
 * 使用 spy 包装被测对象，以便 stub 内部的 lambdaQuery() 链式调用。
 *
 * 覆盖审查报告中指出的缺陷是否已修复：
 *   1. 按店铺拆单 + 金额计算
 *   2. 库存不足/已下架时整体失败（不产生脏订单）
 *   3. 取消订单幂等（并发重复请求不重复回滚库存）
 *   4. 三条取消路径（用户/商家/管理员）均回滚库存
 *   5. 管理员可从「审查中」状态取消
 *   6. 越权隔离
 *   7. createBy 不为空
 *
 * @author ruoyi
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"unchecked", "rawtypes"})
class MarketOrderServiceImplTest
{
    @Mock
    private MarketOrderMapper orderMapper;

    @Mock
    private IMarketCartService cartService;

    @Mock
    private IMarketProductService productService;

    @Mock
    private IMarketOrderItemService orderItemService;

    /** 被测对象（spy，便于 stub 内部链式查询） */
    private MarketOrderServiceImpl orderService;

    private LambdaQueryChainWrapper cartChain;
    private LambdaQueryChainWrapper orderChain;
    private LambdaQueryChainWrapper orderItemChain;

    @BeforeEach
    void setUp() throws Exception
    {
        orderService = Mockito.spy(new MarketOrderServiceImpl());
        // ServiceImpl 的 baseMapper 为 protected 字段，其余依赖为 private，均通过反射注入
        setField(orderService, "baseMapper", orderMapper);
        setField(orderService, "cartService", cartService);
        setField(orderService, "productService", productService);
        setField(orderService, "orderItemService", orderItemService);

        cartChain = newChain();
        orderChain = newChain();
        orderItemChain = newChain();

        // spy 内部方法必须用 doReturn，避免触发真实调用
        Mockito.doReturn(orderChain).when(orderService).lambdaQuery();
        when(cartService.lambdaQuery()).thenReturn(cartChain);
        when(orderItemService.lambdaQuery()).thenReturn(orderItemChain);
    }

    // ===================== 场景1：按店铺拆单 =====================

    @Test
    @DisplayName("下单：购物车含两个店铺的商品，应按店铺拆成两个订单")
    void createOrder_shouldSplitByShop()
    {
        List<MarketCart> carts = Arrays.asList(
                buildCart(1L, 101L, "教材", new BigDecimal("20.00"), 1),
                buildCart(2L, 102L, "耳机", new BigDecimal("30.00"), 2),
                buildCart(3L, 201L, "资料", new BigDecimal("15.00"), 1));

        when(cartChain.list()).thenReturn(carts);
        stubProduct(101L, 10L, "教材", new BigDecimal("20.00"), 100, "0");
        stubProduct(102L, 10L, "耳机", new BigDecimal("30.00"), 100, "0");
        stubProduct(201L, 20L, "资料", new BigDecimal("15.00"), 100, "0");

        MarketOrderCreateBo bo = new MarketOrderCreateBo();
        bo.setCartIds(Arrays.asList(1L, 2L, 3L));
        bo.setReceiverName("张三");

        List<String> orderNos = orderService.createOrder(100L, "zhangsan", bo);

        assertEquals(2, orderNos.size(), "应按店铺拆分为 2 个订单");
        verify(orderMapper, times(2)).insert(any(MarketOrder.class));
        verify(orderItemService, times(3)).save(any(MarketOrderItem.class));
        verify(productService, times(3)).deductStock(anyLong(), any(Integer.class));
        verify(cartService, times(3)).removeById(anyLong());
    }

    @Test
    @DisplayName("下单：订单金额为该店铺商品小计之和，且 createBy 写入用户名")
    void createOrder_shouldCalcTotalAndCreateBy()
    {
        List<MarketCart> carts = Arrays.asList(
                buildCart(1L, 101L, "教材", new BigDecimal("20.00"), 1),
                buildCart(2L, 102L, "耳机", new BigDecimal("30.00"), 2));

        when(cartChain.list()).thenReturn(carts);
        stubProduct(101L, 10L, "教材", new BigDecimal("20.00"), 100, "0");
        stubProduct(102L, 10L, "耳机", new BigDecimal("30.00"), 100, "0");

        List<MarketOrder> saved = new ArrayList<>();
        when(orderMapper.insert(any(MarketOrder.class))).thenAnswer(inv -> {
            saved.add(inv.getArgument(0));
            return 1;
        });

        MarketOrderCreateBo bo = new MarketOrderCreateBo();
        bo.setCartIds(Arrays.asList(1L, 2L));
        orderService.createOrder(100L, "zhangsan", bo);

        assertEquals(1, saved.size(), "同一店铺应合并为 1 个订单");
        // 20*1 + 30*2 = 80
        assertEquals(0, new BigDecimal("80.00").compareTo(saved.get(0).getTotalAmount()),
                "订单总额应为该店铺商品小计之和");
        assertEquals("zhangsan", saved.get(0).getCreateBy(), "createBy 应写入下单用户名");
    }

    // ===================== 场景2：库存不足 / 已下架 =====================

    @Test
    @DisplayName("下单：库存不足时抛业务异常，且不落库订单、不清购物车")
    void createOrder_shouldFailWhenStockInsufficient()
    {
        List<MarketCart> carts = Arrays.asList(
                buildCart(1L, 101L, "教材", new BigDecimal("20.00"), 5));
        when(cartChain.list()).thenReturn(carts);
        // 库存仅 1，但需要 5
        stubProduct(101L, 10L, "教材", new BigDecimal("20.00"), 1, "0");

        MarketOrderCreateBo bo = new MarketOrderCreateBo();
        bo.setCartIds(Arrays.asList(1L));

        assertThrows(ServiceException.class, () -> orderService.createOrder(100L, "zhangsan", bo),
                "库存不足应抛业务异常");
        verify(orderMapper, never()).insert(any(MarketOrder.class));
        verify(orderItemService, never()).save(any(MarketOrderItem.class));
        verify(cartService, never()).removeById(anyLong());
    }

    @Test
    @DisplayName("下单：商品已下架时抛业务异常，不落库订单")
    void createOrder_shouldFailWhenProductOffShelf()
    {
        List<MarketCart> carts = Arrays.asList(
                buildCart(1L, 101L, "教材", new BigDecimal("20.00"), 1));
        when(cartChain.list()).thenReturn(carts);
        stubProduct(101L, 10L, "教材", new BigDecimal("20.00"), 100, "1");

        MarketOrderCreateBo bo = new MarketOrderCreateBo();
        bo.setCartIds(Arrays.asList(1L));

        assertThrows(ServiceException.class, () -> orderService.createOrder(100L, "zhangsan", bo));
        verify(orderMapper, never()).insert(any(MarketOrder.class));
    }

    // ===================== 场景3：取消订单幂等（核心） =====================

    @Test
    @DisplayName("取消订单：CAS 抢占失败时不回滚库存，保证并发重复请求幂等")
    void cancelOrder_shouldBeIdempotent()
    {
        when(orderChain.one()).thenReturn(buildOrder(1L, 100L, 10L, "0"));
        when(orderItemChain.list()).thenReturn(Arrays.asList(buildItem(101L, 2)));
        // 模拟并发：第一个请求抢占成功，第二个请求抢占失败（状态已被改）
        when(orderMapper.updateCancelByCas(1L, "0", "3", "zhangsan")).thenReturn(1, 0);

        int first = orderService.cancelOrder(100L, "zhangsan", 1L);
        int second = orderService.cancelOrder(100L, "zhangsan", 1L);

        assertEquals(1, first, "首次取消应成功");
        assertEquals(0, second, "重复取消应返回 0（幂等）");
        // 关键断言：库存只回滚一次
        verify(productService, times(1)).restoreStock(101L, 2);
    }

    // ===================== 场景4：三条取消路径均回滚库存 =====================

    @Test
    @DisplayName("取消订单：用户取消应回滚库存")
    void cancelOrder_shouldRestoreStock()
    {
        when(orderChain.one()).thenReturn(buildOrder(1L, 100L, 10L, "0"));
        when(orderItemChain.list()).thenReturn(Arrays.asList(buildItem(101L, 2), buildItem(102L, 1)));
        when(orderMapper.updateCancelByCas(1L, "0", "3", "zhangsan")).thenReturn(1);

        int rows = orderService.cancelOrder(100L, "zhangsan", 1L);

        assertEquals(1, rows);
        verify(productService, times(1)).restoreStock(101L, 2);
        verify(productService, times(1)).restoreStock(102L, 1);
    }

    @Test
    @DisplayName("取消订单：商家拒单应回滚库存")
    void merchantCancelOrder_shouldRestoreStock()
    {
        when(orderChain.one()).thenReturn(buildOrder(1L, 100L, 10L, "0"));
        when(orderItemChain.list()).thenReturn(Arrays.asList(buildItem(101L, 3)));
        when(orderMapper.updateCancelByCas(1L, "0", "3", "shopowner")).thenReturn(1);

        int rows = orderService.merchantCancelOrder(10L, "shopowner", 1L);

        assertEquals(1, rows);
        verify(productService, times(1)).restoreStock(101L, 3);
        // 商家拒单必须记录操作人（历史行为要求，修复后回归）
        verify(orderMapper).updateCancelByCas(1L, "0", "3", "shopowner");
    }

    @Test
    @DisplayName("取消订单：管理员仲裁取消应回滚库存并在同一事务内写入审查结论")
    void adminCancelOrder_shouldRestoreStock()
    {
        when(orderMapper.selectById(1L)).thenReturn(buildOrder(1L, 100L, 10L, "0"));
        when(orderItemChain.list()).thenReturn(Arrays.asList(buildItem(101L, 2)));
        when(orderMapper.updateCancelByCasInSources(eq(1L), any(), eq("3"), eq("admin"))).thenReturn(1);

        int rows = orderService.adminCancelOrder(1L, "买家投诉属实", "admin");

        assertEquals(1, rows);
        verify(productService, times(1)).restoreStock(101L, 2);
        // 审查结论由 Service 内部写入（不再由 Controller 二次提交），且带状态守卫 status=3
        verify(orderMapper).updateAuditConclusion(1L, "3", "2", "买家投诉属实", "admin");
    }

    @Test
    @DisplayName("管理员仲裁：CAS 抢占失败时不写审查结论")
    void adminCancelOrder_shouldSkipAuditWhenCasFails()
    {
        when(orderMapper.selectById(1L)).thenReturn(buildOrder(1L, 100L, 10L, "2"));
        when(orderMapper.updateCancelByCasInSources(eq(1L), any(), eq("3"), any())).thenReturn(0);

        assertEquals(0, orderService.adminCancelOrder(1L, "结论", "admin"));

        verify(productService, never()).restoreStock(anyLong(), any(Integer.class));
        verify(orderMapper, never()).updateAuditConclusion(anyLong(), any(), any(), any(), any());
    }

    // ===================== 场景5：管理员可从审查中状态取消 =====================

    @Test
    @DisplayName("管理员仲裁：订单处于审查中(4)时仍可强制取消")
    void adminCancelOrder_shouldAllowFromAuditingStatus()
    {
        when(orderMapper.selectById(1L)).thenReturn(buildOrder(1L, 100L, 10L, "4"));
        when(orderItemChain.list()).thenReturn(Arrays.asList(buildItem(101L, 1)));
        when(orderMapper.updateCancelByCasInSources(eq(1L), any(), eq("3"), any())).thenReturn(1);

        int rows = orderService.adminCancelOrder(1L, "审查结论", "admin");

        assertEquals(1, rows, "审查中状态应允许管理员强制取消");

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(orderMapper).updateCancelByCasInSources(eq(1L), captor.capture(), eq("3"), eq("admin"));
        assertEquals(Arrays.asList("0", "4"), captor.getValue(),
                "允许的源状态应包含待接单(0)与审查中(4)");
        verify(productService, times(1)).restoreStock(101L, 1);
    }

    // ===================== 场景6：越权隔离 =====================

    @Test
    @DisplayName("取消订单：越权操作他人订单应抛异常且不回滚库存")
    void cancelOrder_shouldRejectOthersOrder()
    {
        // 模拟「按攻击者 userId 查询不到该订单」
        when(orderChain.one()).thenReturn(null);

        assertThrows(ServiceException.class, () -> orderService.cancelOrder(999L, "hacker", 1L),
                "越权取消应抛异常");
        verify(productService, never()).restoreStock(anyLong(), any(Integer.class));
        verify(orderMapper, never()).updateStatusByCas(anyLong(), any(), any(), any());
    }

    // ===================== 场景7：确认收货 =====================

    @Test
    @DisplayName("确认收货：待自提→已完成，重复确认幂等返回 0，并记录操作人")
    void confirmOrder_shouldUseCas()
    {
        when(orderChain.one()).thenReturn(buildOrder(1L, 100L, 10L, "1"));
        when(orderMapper.updateConfirmByCas(1L, "1", "2", "zhangsan")).thenReturn(1, 0);

        assertEquals(1, orderService.confirmOrder(100L, "zhangsan", 1L), "首次确认应成功");
        assertEquals(0, orderService.confirmOrder(100L, "zhangsan", 1L), "重复确认应幂等返回 0");
        // 确认收货不应走接单/取消用的通用 CAS（否则 cancel_time 会被误写）
        verify(orderMapper, never()).updateStatusByCas(anyLong(), any(), any(), any());
        verify(orderMapper, never()).updateCancelByCas(anyLong(), any(), any(), any());
    }

    // ===================== 场景8：接单与取消互斥 =====================

    @Test
    @DisplayName("商家接单：走 CAS（0→1），取消已抢先时返回 0，不会复活订单")
    void acceptOrder_shouldUseCas()
    {
        when(orderChain.one()).thenReturn(buildOrder(1L, 100L, 10L, "0"));
        when(orderMapper.updateStatusByCas(1L, "0", "1", "shopowner")).thenReturn(1, 0);

        assertEquals(1, orderService.acceptOrder(10L, "shopowner", 1L), "待接单状态应可接单");
        assertEquals(0, orderService.acceptOrder(10L, "shopowner", 1L), "已被取消的订单不可再接单");
    }

    @Test
    @DisplayName("商家接单：绝不可调用取消专用 CAS，否则会给未取消订单写入 cancel_time")
    void acceptOrder_mustNotWriteCancelTime()
    {
        when(orderChain.one()).thenReturn(buildOrder(1L, 100L, 10L, "0"));
        when(orderMapper.updateStatusByCas(1L, "0", "1", "shopowner")).thenReturn(1);

        orderService.acceptOrder(10L, "shopowner", 1L);

        // 取消专用方法（SQL 内含 cancel_time = now()）一旦被接单复用，订单会出现取消时间
        verify(orderMapper, never()).updateCancelByCas(anyLong(), any(), any(), any());
        verify(orderMapper, never()).updateCancelByCasInSources(anyLong(), any(), any(), any());
        verify(productService, never()).restoreStock(anyLong(), any(Integer.class));
    }

    @Test
    @DisplayName("确认收货：绝不可调用取消专用 CAS")
    void confirmOrder_mustNotWriteCancelTime()
    {
        when(orderChain.one()).thenReturn(buildOrder(1L, 100L, 10L, "1"));
        when(orderMapper.updateConfirmByCas(1L, "1", "2", "zhangsan")).thenReturn(1);

        orderService.confirmOrder(100L, "zhangsan", 1L);

        verify(orderMapper, never()).updateCancelByCas(anyLong(), any(), any(), any());
        verify(orderMapper, never()).updateCancelByCasInSources(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("商家接单：越权操作其他店铺订单应抛异常")
    void acceptOrder_shouldRejectOtherShopOrder()
    {
        when(orderChain.one()).thenReturn(null);

        assertThrows(ServiceException.class, () -> orderService.acceptOrder(999L, "other", 1L));
        verify(orderMapper, never()).updateStatusByCas(anyLong(), any(), any(), any());
    }

    // ===================== 辅助方法 =====================

    private MarketCart buildCart(Long cartId, Long productId, String name, BigDecimal price, Integer qty)
    {
        MarketCart cart = new MarketCart();
        cart.setCartId(cartId);
        cart.setUserId(100L);
        cart.setProductId(productId);
        cart.setProductName(name);
        cart.setPrice(price);
        cart.setQuantity(qty);
        return cart;
    }

    private MarketProduct buildProduct(Long productId, Long shopId, String name, BigDecimal price,
            Integer stock, String status)
    {
        MarketProduct product = new MarketProduct();
        product.setProductId(productId);
        product.setShopId(shopId);
        product.setProductName(name);
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus(status);
        return product;
    }

    private MarketOrder buildOrder(Long orderId, Long userId, Long shopId, String status)
    {
        MarketOrder order = new MarketOrder();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setStatus(status);
        return order;
    }

    private MarketOrderItem buildItem(Long productId, Integer qty)
    {
        MarketOrderItem item = new MarketOrderItem();
        item.setItemId(productId);
        item.setProductId(productId);
        item.setQuantity(qty);
        return item;
    }

    private void stubProduct(Long productId, Long shopId, String name, BigDecimal price,
            Integer stock, String status)
    {
        when(productService.getById(productId))
                .thenReturn(buildProduct(productId, shopId, name, price, stock, status));
    }

    /**
     * 构造链式查询 mock
     *
     * 本版本链式调用实际走到 Compare.eq(Object column, Object val) 与 Func.in(Object, Collection)，
     * 故按两参签名 stub，保证链式调用返回自身而不中断。
     */
    private LambdaQueryChainWrapper newChain()
    {
        LambdaQueryChainWrapper chain = Mockito.mock(LambdaQueryChainWrapper.class);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.in(any(), anyCollection())).thenReturn(chain);
        when(chain.orderByDesc(any())).thenReturn(chain);
        return chain;
    }

    private <T> java.util.Collection<T> anyCollection()
    {
        return org.mockito.ArgumentMatchers.anyCollection();
    }

    /** 通过反射设置对象字段（用于注入 protected 的 baseMapper 与私有依赖） */
    private void setField(Object target, String fieldName, Object value) throws Exception
    {
        Class<?> clazz = target.getClass();
        while (clazz != null)
        {
            try
            {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            }
            catch (NoSuchFieldException e)
            {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
