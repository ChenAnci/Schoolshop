package com.ruoyi.market.service;

import java.util.List;
import com.baomidou.mybatisplus.spring.service.IService;
import com.ruoyi.market.domain.MarketReview;

/**
 * 商品评价 服务层
 *
 * @author ruoyi
 */
public interface IMarketReviewService extends IService<MarketReview>
{
    /**
     * 按商品查询评价列表
     *
     * @param review 评价（productId 必填）
     * @return 评价集合
     */
    public List<MarketReview> selectProductReviewList(MarketReview review);
}