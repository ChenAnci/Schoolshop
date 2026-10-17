package com.ruoyi.market.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.market.domain.MarketCart;
import com.ruoyi.market.domain.MarketProduct;
import com.ruoyi.market.mapper.MarketCartMapper;
import com.ruoyi.market.service.IMarketCartService;
import com.ruoyi.market.service.IMarketProductService;

/**
 * 购物车 服务层实现
 *
 * 承载购物车的业务规则：库存校验、同商品累加、归属校验（越权操作直接拒绝）。
 *
 * @author ruoyi
 */
@Service
public class MarketCartServiceImpl extends ServiceImpl<MarketCartMapper, MarketCart>
        implements IMarketCartService
{
    @Autowired
    private IMarketProductService productService;

    @Override
    public List<MarketCart> selectCartList(MarketCart cart)
    {
        return baseMapper.selectCartList(cart);
    }

    @Override
    public int addItem(Long userId, Long productId, Integer quantity)
    {
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        if (productId == null)
        {
            throw new ServiceException("商品不能为空");
        }
        int qty = (quantity == null || quantity < 1) ? 1 : quantity;

        MarketProduct product = productService.getById(productId);
        if (product == null)
        {
            throw new ServiceException("商品不存在");
        }
        if (product.getStock() == null || product.getStock() < qty)
        {
            throw new ServiceException("库存不足");
        }

        Date now = DateUtils.getNowDate();
        // 同一商品已存在则累加数量
        MarketCart exist = this.lambdaQuery()
                .eq(MarketCart::getUserId, userId)
                .eq(MarketCart::getProductId, productId)
                .one();
        if (exist != null)
        {
            int newQty = (exist.getQuantity() == null ? 0 : exist.getQuantity()) + qty;
            if (newQty > product.getStock())
            {
                throw new ServiceException("库存不足");
            }
            MarketCart update = new MarketCart();
            update.setCartId(exist.getCartId());
            update.setQuantity(newQty);
            update.setUpdateTime(now);
            return baseMapper.updateById(update);
        }

        MarketCart add = new MarketCart();
        add.setUserId(userId);
        add.setProductId(product.getProductId());
        add.setProductName(product.getProductName());
        add.setProductImage(product.getProductImage());
        add.setPrice(product.getPrice());
        add.setQuantity(qty);
        add.setChecked("1");
        add.setCreateTime(now);
        add.setUpdateTime(now);
        return baseMapper.insert(add);
    }

    @Override
    public int updateQuantity(Long userId, Long cartId, Integer quantity)
    {
        if (cartId == null)
        {
            throw new ServiceException("参数不合法");
        }
        MarketCart own = getOwnCartOrNull(userId, cartId);
        if (own == null)
        {
            throw new ServiceException("购物车条目不存在");
        }
        int qty = (quantity == null || quantity < 1) ? 1 : quantity;
        MarketProduct product = productService.getById(own.getProductId());
        if (product != null && product.getStock() != null && qty > product.getStock())
        {
            throw new ServiceException("库存不足");
        }
        MarketCart update = new MarketCart();
        update.setCartId(cartId);
        update.setQuantity(qty);
        update.setUpdateTime(DateUtils.getNowDate());
        return baseMapper.updateById(update);
    }

    @Override
    public int removeItems(Long userId, Long[] cartIds)
    {
        if (userId == null || cartIds == null || cartIds.length == 0)
        {
            return 0;
        }
        // 仅删除属于当前用户的条目，越权 ID 自动忽略
        List<Long> owned = new ArrayList<>();
        for (Long id : cartIds)
        {
            if (id != null && getOwnCartOrNull(userId, id) != null)
            {
                owned.add(id);
            }
        }
        if (owned.isEmpty())
        {
            return 0;
        }
        return baseMapper.deleteBatchIds(owned);
    }

    @Override
    public boolean clear(Long userId)
    {
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        this.lambdaUpdate().eq(MarketCart::getUserId, userId).remove();
        return true;
    }

    @Override
    public MarketCart getOwnCartOrNull(Long userId, Long cartId)
    {
        return this.lambdaQuery()
                .eq(MarketCart::getCartId, cartId)
                .eq(MarketCart::getUserId, userId)
                .one();
    }
}
