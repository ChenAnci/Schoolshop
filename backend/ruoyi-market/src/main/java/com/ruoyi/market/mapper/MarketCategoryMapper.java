package com.ruoyi.market.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.market.domain.MarketCategory;

/**
 * 商品分类 数据层
 * 
 * @author ruoyi
 */
public interface MarketCategoryMapper extends BaseMapper<MarketCategory>
{
    /**
     * 查询商品分类列表
     * 
     * @param category 商品分类
     * @return 商品分类集合
     */
    public List<MarketCategory> selectCategoryList(MarketCategory category);
}
