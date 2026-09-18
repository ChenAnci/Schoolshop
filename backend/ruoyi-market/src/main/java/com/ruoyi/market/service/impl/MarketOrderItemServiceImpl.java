package com.ruoyi.market.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ruoyi.market.domain.MarketOrderItem;
import com.ruoyi.market.mapper.MarketOrderItemMapper;
import com.ruoyi.market.service.IMarketOrderItemService;

/**
 * 订单明细 服务层实现
 *
 * @author ruoyi
 */
@Service
public class MarketOrderItemServiceImpl extends ServiceImpl<MarketOrderItemMapper, MarketOrderItem>
        implements IMarketOrderItemService
{
    /**
     * 查询订单明细列表
     *
     * @param orderItem 订单明细
     * @return 订单明细集合
     */
    @Override
    public List<MarketOrderItem> selectOrderItemList(MarketOrderItem orderItem)
    {
        return baseMapper.selectOrderItemList(orderItem);
    }
}