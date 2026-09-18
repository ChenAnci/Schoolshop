package com.ruoyi.market.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ruoyi.market.domain.MarketProduct;
import com.ruoyi.market.mapper.MarketProductMapper;
import com.ruoyi.market.service.IMarketProductService;

/**
 * 商品信息 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class MarketProductServiceImpl extends ServiceImpl<MarketProductMapper, MarketProduct>
        implements IMarketProductService
{
    /**
     * 查询商品信息列表
     * 
     * @param product 商品信息
     * @return 商品信息集合
     */
    @Override
    public List<MarketProduct> selectProductList(MarketProduct product)
    {
        return baseMapper.selectProductList(product);
    }
}
