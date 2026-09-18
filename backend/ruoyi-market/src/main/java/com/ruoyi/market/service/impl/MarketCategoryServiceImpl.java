package com.ruoyi.market.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ruoyi.market.domain.MarketCategory;
import com.ruoyi.market.mapper.MarketCategoryMapper;
import com.ruoyi.market.service.IMarketCategoryService;

/**
 * 商品分类 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class MarketCategoryServiceImpl extends ServiceImpl<MarketCategoryMapper, MarketCategory>
        implements IMarketCategoryService
{
    /**
     * 查询商品分类列表
     * 
     * @param category 商品分类
     * @return 商品分类集合
     */
    @Override
    public List<MarketCategory> selectCategoryList(MarketCategory category)
    {
        return baseMapper.selectCategoryList(category);
    }
}
