package com.ruoyi.market.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ruoyi.market.domain.MarketOrder;
import com.ruoyi.market.mapper.MarketOrderMapper;
import com.ruoyi.market.service.IMarketOrderService;

/**
 * 订单 服务层实现
 *
 * @author ruoyi
 */
@Service
public class MarketOrderServiceImpl extends ServiceImpl<MarketOrderMapper, MarketOrder>
        implements IMarketOrderService
{
    /**
     * 查询订单列表
     *
     * @param order 订单
     * @return 订单集合
     */
    @Override
    public List<MarketOrder> selectOrderList(MarketOrder order)
    {
        return baseMapper.selectOrderList(order);
    }
}