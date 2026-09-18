package com.ruoyi.market.service;

import java.util.List;
import com.baomidou.mybatisplus.spring.service.IService;
import com.ruoyi.market.domain.MarketCategory;

/**
 * 商品分类 服务层
 * 
 * @author ruoyi
 */
public interface IMarketCategoryService extends IService<MarketCategory>
{
    /**
     * 查询商品分类列表
     * 
     * @param category 商品分类
     * @return 商品分类集合
     */
    public List<MarketCategory> selectCategoryList(MarketCategory category);
}
