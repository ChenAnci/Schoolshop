package com.ruoyi.market.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
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

    /**
     * 原子扣减库存并累加销量（乐观锁 CAS，防超卖）
     *
     * 说明：使用「条件更新」而非「先查再改」，把库存判断与扣减合并为一条 SQL。
     * MySQL 的行级锁保证同一商品的并发扣减串行执行，stock >= #{quantity} 保证不会扣成负数。
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return 影响行数，0 表示库存不足或商品不存在（调用方应据此回滚事务）
     */
    public int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 原子回滚库存并回退销量（订单取消/超时关闭时使用）
     *
     * @param productId 商品ID
     * @param quantity  回滚数量
     * @return 影响行数
     */
    public int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
