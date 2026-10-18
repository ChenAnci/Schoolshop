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
    public List<String> createOrder(Long userId, String username, MarketOrderCreateBo bo);

    /**
     * 取消订单（用户侧，仅待接单状态可取消），并回滚库存
     *
     * @param userId   当前登录用户ID
     * @param username 当前登录用户名（写入 update_by 供审计）
     * @param orderId  订单ID
     * @return 影响行数，0 表示状态不符或已被并发处理（幂等，不重复回滚）
     */
    public int cancelOrder(Long userId, String username, Long orderId);

    /**
     * 商家拒单（仅待接单状态可拒单），并回滚库存
     *
     * @param shopId   当前商家店铺ID
     * @param username 当前登录用户名（写入 update_by 供审计）
     * @param orderId  订单ID
     * @return 影响行数，0 表示状态不符或已被并发处理
     */
    public int merchantCancelOrder(Long shopId, String username, Long orderId);

    /**
     * 商家接单：待接单 → 待自提
     *
     * 使用 CAS 条件更新，与取消互斥：若取消已抢先完成（状态已变），接单返回 0，
     * 避免「取消回滚了库存、接单又把订单改回待自提」导致订单复活而库存偏高。
     *
     * @param shopId   当前商家店铺ID
     * @param username 当前登录用户名（写入 update_by 供审计）
     * @param orderId  订单ID
     * @return 影响行数，0 表示状态不符或已被并发处理
     */
    public int acceptOrder(Long shopId, String username, Long orderId);

    /**
     * 管理员仲裁取消订单，并回滚库存
     *
     * 取消与审查结论（audit_flag/audit_remark）在同一事务内完成，避免订单已取消但结论未落库。
     *
     * @param orderId     订单ID
     * @param auditRemark 仲裁意见
     * @param username    当前登录用户名（写入 update_by 供审计）
     * @return 影响行数，0 表示状态不符或已被并发处理
     */
    public int adminCancelOrder(Long orderId, String auditRemark, String username);

    /**
     * 管理员仲裁：强制已完成（仅「审查中」可仲裁）
     *
     * 使用 CAS 条件更新（4→2），与 cancel 分支保持一致的一致性模型：
     * 并发仲裁时只有一个请求生效，其余返回 0，不会互相覆盖状态与审查结论。
     *
     * @param orderId     订单ID
     * @param auditRemark 仲裁意见
     * @param username    当前登录用户名（写入 update_by 供审计）
     * @return 影响行数，0 表示状态不符或已被并发处理
     */
    public int adminCompleteOrder(Long orderId, String auditRemark, String username);

    /**
     * 管理员仲裁：恢复流转（仅「审查中」可恢复，回到待商家接单）
     *
     * 使用 CAS 条件更新（4→0），语义与其他仲裁分支一致。
     *
     * @param orderId     订单ID
     * @param auditRemark 仲裁意见
     * @param username    当前登录用户名（写入 update_by 供审计）
     * @return 影响行数，0 表示状态不符或已被并发处理
     */
    public int adminRestoreOrder(Long orderId, String auditRemark, String username);

    /**
     * 确认收货（提货）：待自提 → 已完成
     *
     * @param userId   当前登录用户ID
     * @param username 当前登录用户名（写入 update_by 供审计）
     * @param orderId  订单ID
     * @return 影响行数，0 表示状态不符或已被并发处理
     */
    public int confirmOrder(Long userId, String username, Long orderId);

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
