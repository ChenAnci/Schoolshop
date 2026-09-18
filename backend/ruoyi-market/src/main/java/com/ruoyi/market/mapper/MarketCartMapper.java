package com.ruoyi.market.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.market.domain.MarketCart;

/**
 * 购物车 数据层
 *
 * @author ruoyi
 */
public interface MarketCartMapper extends BaseMapper<MarketCart>
{
    /**
     * 查询购物车列表
     *
     * @param cart 购物车
     * @return 购物车集合
     */
    public List<MarketCart> selectCartList(MarketCart cart);
}