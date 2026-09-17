package com.ruoyi.market.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.market.domain.MarketProduct;

/**
 * 商品信息 数据层
 * 
 * @author ruoyi
 */
public interface MarketProductMapper extends BaseMapper<MarketProduct>
{
    /**
     * 查询商品信息列表（关联分类名称）
     * 
     * @param product 商品信息
     * @return 商品信息集合
     */
    public List<MarketProduct> selectProductList(MarketProduct product);
}
