package com.ruoyi.market.service;

import java.util.List;
import com.baomidou.mybatisplus.spring.service.IService;
import com.ruoyi.market.domain.MarketShop;

/**
 * 店铺信息 服务层
 *
 * @author ruoyi
 */
public interface IMarketShopService extends IService<MarketShop>
{
    /**
     * 查询店铺信息列表
     *
     * @param shop 店铺信息
     * @return 店铺信息集合
     */
    public List<MarketShop> selectShopList(MarketShop shop);
}