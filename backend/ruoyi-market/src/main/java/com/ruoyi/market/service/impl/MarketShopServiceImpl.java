package com.ruoyi.market.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ruoyi.market.domain.MarketShop;
import com.ruoyi.market.mapper.MarketShopMapper;
import com.ruoyi.market.service.IMarketShopService;

/**
 * 店铺信息 服务层实现
 *
 * @author ruoyi
 */
@Service
public class MarketShopServiceImpl extends ServiceImpl<MarketShopMapper, MarketShop>
        implements IMarketShopService
{
    /**
     * 查询店铺信息列表
     *
     * @param shop 店铺信息
     * @return 店铺信息集合
     */
    @Override
    public List<MarketShop> selectShopList(MarketShop shop)
    {
        return baseMapper.selectShopList(shop);
    }
}