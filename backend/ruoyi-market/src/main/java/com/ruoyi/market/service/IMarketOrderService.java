package com.ruoyi.market.service;

import java.util.List;
import com.baomidou.mybatisplus.spring.service.IService;
import com.ruoyi.market.domain.MarketOrder;
import com.ruoyi.market.domain.MarketOrderCreateBo;

/**
 * 订单 服务层
 *
 * 说明：业务规则（状态校验、库存扣减、事务边界）收敛在本层，Controller 只做参数接收与结果包装。
 *
 * @author ruoyi
 */
public interface IMarketOrderService extends IService<MarketOrder>
{
    /**
     * 查询订单列表
     *
     * @param order 订单
     * @return 订单集合
     */
    public List<MarketOrder> selectOrderList(MarketOrder order);

    /**
     * 结算下单：按店铺拆分购物车条目生成订单
     *
     * 事务边界在本方法（而非 Controller）：创建订单 + 明细 + 扣库存 + 清购物车，任一失败整体回滚。
     *
     * @param userId 当前登录用户ID（由 Service 接收，避免 Service 依赖 Web 层上下文）
     * @param bo     下单参数
     * @return 生成的订单编号集合（一个店铺一个订单）
     */
    public List<String> createOrder(Long userId, MarketOrderCreateBo bo);

    /**
     * 取消订单（仅待接单状态可取消，并回滚库存）
     *
     * @param userId  当前登录用户ID
     * @param orderId 订单ID
     * @return 影响行数
     */
    public int cancelOrder(Long userId, Long orderId);

    /**
     * 确认收货（提货）：待自提 → 已完成
     *
     * @param userId  当前登录用户ID
     * @param orderId 订单ID
     * @return 影响行数
     */
    public int confirmOrder(Long userId, Long orderId);

    /**
     * 查询当前用户的订单列表（可按状态筛选）
     *
     * @param userId 当前登录用户ID
     * @param status 订单状态，为空表示全部
     * @return 订单集合
     */
    public List<MarketOrder> listOwnOrders(Long userId, String status);

    /**
     * 查询当前用户的订单，不存在时返回 null（供只读查询使用，不做异常抛出）
     *
     * @param userId  当前登录用户ID
     * @param orderId 订单ID
     * @return 订单，非当前用户订单返回 null
     */
    public MarketOrder getOwnOrderOrNull(Long userId, Long orderId);
}
