package com.ruoyi.market.service;

import java.util.List;
import com.baomidou.mybatisplus.spring.service.IService;
import com.ruoyi.market.domain.MarketCart;

/**
 * 购物车 服务层
 *
 * @author ruoyi
 */
public interface IMarketCartService extends IService<MarketCart>
{
    /**
     * 查询购物车列表
     *
     * @param cart 购物车
     * @return 购物车集合
     */
    public List<MarketCart> selectCartList(MarketCart cart);
}