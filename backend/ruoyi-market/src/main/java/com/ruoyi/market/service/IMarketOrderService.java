package com.ruoyi.market.service;

import java.util.List;
import com.baomidou.mybatisplus.spring.service.IService;
import com.ruoyi.market.domain.MarketOrder;

/**
 * 订单 服务层
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
}