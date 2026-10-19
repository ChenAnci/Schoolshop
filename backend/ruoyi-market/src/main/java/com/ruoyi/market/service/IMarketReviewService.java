package com.ruoyi.market.service;

import java.util.List;
import com.baomidou.mybatisplus.spring.service.IService;
import com.ruoyi.market.domain.MarketReview;
import com.ruoyi.market.domain.MarketReviewSubmitBo;

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

    /**
     * 提交评价：对已完成订单的若干明细进行评价（每条明细仅可评价一次）
     *
     * @param userId 当前登录用户ID
     * @param bo     评价入参
     * @return 实际新增的评价条数
     */
    public int submitReviews(Long userId, MarketReviewSubmitBo bo);
}