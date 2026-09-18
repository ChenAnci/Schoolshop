package com.ruoyi.market.service;

import java.util.List;
import com.baomidou.mybatisplus.spring.service.IService;
import com.ruoyi.market.domain.MarketProduct;

/**
 * 商品信息 服务层
 * 
 * @author ruoyi
 */
public interface IMarketProductService extends IService<MarketProduct>
{
    /**
     * 查询商品信息列表
     * 
     * @param product 商品信息
     * @return 商品信息集合
     */
    public List<MarketProduct> selectProductList(MarketProduct product);
}
