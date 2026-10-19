package com.ruoyi.market.service;

import java.util.List;
import com.baomidou.mybatisplus.spring.service.IService;
import com.ruoyi.market.domain.MarketCart;

/**
 * 购物车 服务层
 *
 * @author ruoyi
 */
public interface IMarketCartService extends IService<MarketCart>
{
    /**
     * 查询购物车列表
     *
     * @param cart 购物车
     * @return 购物车集合
     */
    public List<MarketCart> selectCartList(MarketCart cart);

    /**
     * 加入购物车（已存在同一商品则累加数量）
     *
     * @param userId    当前登录用户ID
     * @param productId 商品ID
     * @param quantity  数量
     * @return 影响行数
     */
    public int addItem(Long userId, Long productId, Integer quantity);

    /**
     * 修改购物车条目数量
     *
     * @param userId  当前登录用户ID
     * @param cartId  购物车条目ID
     * @param quantity 目标数量
     * @return 影响行数
     */
    public int updateQuantity(Long userId, Long cartId, Integer quantity);

    /**
     * 删除当前用户的购物车条目（越权条目自动忽略）
     *
     * @param userId  当前登录用户ID
     * @param cartIds 购物车条目ID集合
     * @return 影响行数
     */
    public int removeItems(Long userId, Long[] cartIds);

    /**
     * 清空当前用户购物车
     *
     * @param userId 当前登录用户ID
     * @return 是否成功
     */
    public boolean clear(Long userId);

    /**
     * 查询当前用户的购物车条目，不存在返回 null（归属校验）
     *
     * @param userId 当前登录用户ID
     * @param cartId 购物车条目ID
     * @return 购物车条目
     */
    public MarketCart getOwnCartOrNull(Long userId, Long cartId);
}
