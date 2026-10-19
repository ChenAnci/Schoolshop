package com.ruoyi.market.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
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

    @Override
    public void deductStock(Long productId, Integer quantity)
    {
        if (productId == null || quantity == null || quantity <= 0)
        {
            throw new ServiceException("扣减库存参数不合法");
        }
        int rows = baseMapper.deductStock(productId, quantity);
        if (rows <= 0)
        {
            // 影响行数为 0：要么商品不存在/已下架，要么库存不足
            MarketProduct product = baseMapper.selectById(productId);
            if (product == null)
            {
                throw new ServiceException("商品不存在");
            }
            if (!"0".equals(product.getStatus()))
            {
                throw new ServiceException("商品「" + product.getProductName() + "」已下架");
            }
            throw new ServiceException("商品「" + product.getProductName() + "」库存不足");
        }
    }

    @Override
    public void restoreStock(Long productId, Integer quantity)
    {
        if (productId == null || quantity == null || quantity <= 0)
        {
            throw new ServiceException("回滚库存参数不合法");
        }
        int rows = baseMapper.restoreStock(productId, quantity);
        if (rows <= 0)
        {
            // 影响行数为 0 说明商品已不存在，属于数据异常，必须让调用方感知并回滚事务
            throw new ServiceException("回滚库存失败：商品不存在");
        }
    }
}
