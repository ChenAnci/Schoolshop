package com.ruoyi.market.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.market.domain.MarketOrderItem;

/**
 * 订单明细 数据层
 *
 * @author ruoyi
 */
public interface MarketOrderItemMapper extends BaseMapper<MarketOrderItem>
{
    /**
     * 查询订单明细列表
     *
     * @param orderItem 订单明细
     * @return 订单明细集合
     */
    public List<MarketOrderItem> selectOrderItemList(MarketOrderItem orderItem);
}