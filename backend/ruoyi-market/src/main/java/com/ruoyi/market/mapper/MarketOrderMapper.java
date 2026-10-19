package com.ruoyi.market.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.market.domain.MarketOrder;

/**
 * 订单 数据层
 *
 * 状态流转相关的更新一律使用条件更新（CAS），影响行数为 0 即代表状态不符或已被并发处理，
 * 调用方据此判断是否执行库存回滚等副作用，避免重复执行。
 *
 * 注意：不同流转会写不同的业务时间字段（取消写 cancel_time、确认写 finish_time），
 * 故按流转类型拆分方法，禁止用一个通用 CAS 同时服务多种流转。
 *
 * @author ruoyi
 */
public interface MarketOrderMapper extends BaseMapper<MarketOrder>
{
    /**
     * 查询订单列表
     *
     * @param order 订单
     * @return 订单集合
     */
    public List<MarketOrder> selectOrderList(MarketOrder order);

    /**
     * 条件更新订单状态（CAS，通用，不写任何业务时间字段）
     *
     * 仅当订单当前状态等于 expectedStatus 时才更新为 targetStatus，
     * 影响行数为 1 表示流转成功，为 0 表示订单不存在或状态已被其他请求变更。
     *
     * 用于「接单」这类只改状态、不需要额外业务时间的流转。
     * 取消请使用 {@link #updateCancelByCas}，确认收货请使用 {@link #updateConfirmByCas}。
     *
     * @param orderId        订单ID
     * @param expectedStatus 期望的当前状态
     * @param targetStatus   目标状态
     * @param updateBy       操作人，为空时不覆盖
     * @return 影响行数（0 或 1）
     */
    public int updateStatusByCas(@Param("orderId") Long orderId,
            @Param("expectedStatus") String expectedStatus,
            @Param("targetStatus") String targetStatus,
            @Param("updateBy") String updateBy);

    /**
     * 条件取消订单（CAS）：同时写入 cancel_time
     *
     * 仅取消流转会写 cancel_time，接单/确认等其他流转不得复用本方法，
     * 否则会在未取消的订单上留下取消时间。
     *
     * @param orderId        订单ID
     * @param expectedStatus 期望的当前状态
     * @param targetStatus   目标状态
     * @param updateBy       操作人，为空时不覆盖
     * @return 影响行数（0 或 1）
     */
    public int updateCancelByCas(@Param("orderId") Long orderId,
            @Param("expectedStatus") String expectedStatus,
            @Param("targetStatus") String targetStatus,
            @Param("updateBy") String updateBy);

    /**
     * 条件取消订单（CAS，支持多个合法源状态）：同时写入 cancel_time
     *
     * 用于管理员仲裁这类「可从多个状态强制取消」的场景。
     *
     * @param orderId          订单ID
     * @param expectedStatuses 允许的当前状态集合
     * @param targetStatus     目标状态
     * @param updateBy         操作人，为空时不覆盖
     * @return 影响行数（0 或 1）
     */
    public int updateCancelByCasInSources(@Param("orderId") Long orderId,
            @Param("expectedStatuses") List<String> expectedStatuses,
            @Param("targetStatus") String targetStatus,
            @Param("updateBy") String updateBy);

    /**
     * 条件确认收货（CAS）：待自提 → 已完成，同时写入 finish_time
     *
     * 完成时间用 ifnull 保留首次写入值，语义对齐原实现「若为空则写入 now()」：
     * 既保证确认后一定有完成时间，也避免重复提交刷掉首次完成时间。
     *
     * @param orderId        订单ID
     * @param expectedStatus 期望的当前状态
     * @param targetStatus   目标状态
     * @param updateBy       操作人，为空时不覆盖
     * @return 影响行数（0 或 1）
     */
    public int updateConfirmByCas(@Param("orderId") Long orderId,
            @Param("expectedStatus") String expectedStatus,
            @Param("targetStatus") String targetStatus,
            @Param("updateBy") String updateBy);

    /**
     * 更新订单审查结论（仲裁成功后写入 audit_flag / audit_remark）
     *
     * 带状态守卫：仅当订单确实处于 expectedStatus 时才写入，
     * 防止被复用到其他场景时把「未审查」订单直接标成「已仲裁」。
     *
     * @param orderId        订单ID
     * @param expectedStatus 期望的当前状态（仲裁取消后应为已取消 3）
     * @param auditFlag      审查标记（2 已仲裁）
     * @param auditRemark    审查意见
     * @param updateBy       操作人
     * @return 影响行数（0 或 1）
     */
    public int updateAuditConclusion(@Param("orderId") Long orderId,
            @Param("expectedStatus") String expectedStatus,
            @Param("auditFlag") String auditFlag,
            @Param("auditRemark") String auditRemark,
            @Param("updateBy") String updateBy);

    /**
     * 进入审查（CAS）：把订单置为审查中(4)并写 audit_flag=1，同时排除指定源状态
     *
     * 「进入审查」不对源状态做白名单，只排除「已在审查流程中」的状态（4 审查中、5 审查完成），
     * 因此这里用 NOT IN 而不是 = expectedStatus。
     *
     * 用 CAS 而非整对象 updateById 的原因：整行回写会把读取瞬间的旧状态一起写回，
     * 并发下会覆盖管理员仲裁或其他路径刚完成的状态变更（例如把已取消订单改回审查中）。
     *
     * @param orderId         订单ID
     * @param excludedStatuses 不允许作为源状态的状态集合（4/5）
     * @param targetStatus    目标状态（4 审查中）
     * @param auditFlag       审查标记（1 审查中）
     * @param auditRemark     审查原因
     * @param updateBy        操作人，为空时不覆盖
     * @return 影响行数（0 或 1）
     */
    public int updateStartAuditByCas(@Param("orderId") Long orderId,
            @Param("excludedStatuses") List<String> excludedStatuses,
            @Param("targetStatus") String targetStatus,
            @Param("auditFlag") String auditFlag,
            @Param("auditRemark") String auditRemark,
            @Param("updateBy") String updateBy);
}
