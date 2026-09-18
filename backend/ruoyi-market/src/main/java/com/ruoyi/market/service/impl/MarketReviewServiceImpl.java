package com.ruoyi.market.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ruoyi.market.domain.MarketReview;
import com.ruoyi.market.mapper.MarketReviewMapper;
import com.ruoyi.market.service.IMarketReviewService;

/**
 * 商品评价 服务层实现
 *
 * @author ruoyi
 */
@Service
public class MarketReviewServiceImpl extends ServiceImpl<MarketReviewMapper, MarketReview>
        implements IMarketReviewService
{
    /**
     * 按商品查询评价列表
     *
     * @param review 评价（productId 必填）
     * @return 评价集合
     */
    @Override
    public List<MarketReview> selectProductReviewList(MarketReview review)
    {
        return baseMapper.selectProductReviewList(review);
    }
}