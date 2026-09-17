package com.ruoyi.market.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ruoyi.market.domain.MarketCart;
import com.ruoyi.market.mapper.MarketCartMapper;
import com.ruoyi.market.service.IMarketCartService;

/**
 * 购物车 服务层实现
 *
 * @author ruoyi
 */
@Service
public class MarketCartServiceImpl extends ServiceImpl<MarketCartMapper, MarketCart>
        implements IMarketCartService
{
    /**
     * 查询购物车列表
     *
     * @param cart 购物车
     * @return 购物车集合
     */
    @Override
    public List<MarketCart> selectCartList(MarketCart cart)
    {
        return baseMapper.selectCartList(cart);
    }
}