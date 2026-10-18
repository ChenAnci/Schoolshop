package com.ruoyi.market.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.market.domain.MarketOrder;

/**
 * 订单 数据层
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
     * 条件更新订单状态（CAS，用于状态机流转的幂等控制）
     *
     * 仅当订单当前状态等于 expectedStatus 时才更新为 targetStatus，
     * 影响行数为 1 表示流转成功，为 0 表示订单不存在或状态已被其他请求变更。
     * 这样可避免并发重复请求重复执行库存回滚等副作用。
     *
     * 同时记录操作人（update_by）与取消时间，便于审计与追溯。
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
     * 条件更新订单状态（CAS，支持多个合法源状态）
     *
     * 用于管理员仲裁这类「可从多个状态强制流转到终态」的场景：
     * 仅当订单当前状态属于 expectedStatuses 之一时才流转，
     * 影响行数为 1 表示流转成功，为 0 表示状态不符或已被并发处理。
     *
     * 同时记录操作人（update_by）与取消时间，便于审计与追溯。
     *
     * @param orderId          订单ID
     * @param expectedStatuses 允许的当前状态集合
     * @param targetStatus     目标状态
     * @param updateBy         操作人，为空时不覆盖
     * @return 影响行数（0 或 1）
     */
    public int updateStatusByCasInSources(@Param("orderId") Long orderId,
            @Param("expectedStatuses") List<String> expectedStatuses,
            @Param("targetStatus") String targetStatus,
            @Param("updateBy") String updateBy);

    /**
     * 条件确认收货（CAS）：待自提 → 已完成
     *
     * 完成时间 finish_time 用 ifnull 保留首次写入值，语义对齐原实现「若为空则写入 now()」：
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
     * @param orderId     订单ID
     * @param auditFlag   审查标记（2 已仲裁）
     * @param auditRemark 审查意见
     * @param updateBy    操作人
     * @return 影响行数（0 或 1）
     */
    public int updateAuditConclusion(@Param("orderId") Long orderId,
            @Param("auditFlag") String auditFlag,
            @Param("auditRemark") String auditRemark,
            @Param("updateBy") String updateBy);
}
