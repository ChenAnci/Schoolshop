package com.ruoyi.market.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.market.domain.MarketShop;

/**
 * 店铺信息 数据层
 *
 * @author ruoyi
 */
public interface MarketShopMapper extends BaseMapper<MarketShop>
{
    /**
     * 查询店铺信息列表
     *
     * @param shop 店铺信息
     * @return 店铺信息集合
     */
    public List<MarketShop> selectShopList(MarketShop shop);
}