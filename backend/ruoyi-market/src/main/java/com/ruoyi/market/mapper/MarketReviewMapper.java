package com.ruoyi.market.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.market.domain.MarketReview;

/**
 * 商品评价 数据层
 *
 * @author ruoyi
 */
public interface MarketReviewMapper extends BaseMapper<MarketReview>
{
    /**
     * 按商品查询评价列表（关联用户昵称、店铺名）
     *
     * @param review 评价（productId 必填）
     * @return 评价集合
     */
    public List<MarketReview> selectProductReviewList(MarketReview review);
}