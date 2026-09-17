package com.ruoyi.market.service;

import java.util.List;
import com.baomidou.mybatisplus.spring.service.IService;
import com.ruoyi.market.domain.MarketOrderItem;

/**
 * 订单明细 服务层
 *
 * @author ruoyi
 */
public interface IMarketOrderItemService extends IService<MarketOrderItem>
{
    /**
     * 查询订单明细列表
     *
     * @param orderItem 订单明细
     * @return 订单明细集合
     */
    public List<MarketOrderItem> selectOrderItemList(MarketOrderItem orderItem);
}