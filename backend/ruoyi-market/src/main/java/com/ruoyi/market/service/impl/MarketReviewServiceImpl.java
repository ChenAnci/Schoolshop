package com.ruoyi.market.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.market.domain.MarketOrder;
import com.ruoyi.market.domain.MarketOrderItem;
import com.ruoyi.market.domain.MarketReview;
import com.ruoyi.market.domain.MarketReviewSubmitBo;
import com.ruoyi.market.mapper.MarketReviewMapper;
import com.ruoyi.market.service.IMarketOrderItemService;
import com.ruoyi.market.service.IMarketOrderService;
import com.ruoyi.market.service.IMarketReviewService;

/**
 * 商品评价 服务层实现
 *
 * 承载评价的业务规则：订单归属校验、仅已完成订单可评价、同一订单明细不可重复评价。
 *
 * @author ruoyi
 */
@Service
public class MarketReviewServiceImpl extends ServiceImpl<MarketReviewMapper, MarketReview>
        implements IMarketReviewService
{
    /** 订单状态：已完成（仅已完成订单可评价） */
    private static final String STATUS_FINISHED = "2";

    @Autowired
    private IMarketOrderService orderService;

    @Autowired
    private IMarketOrderItemService orderItemService;

    @Override
    public List<MarketReview> selectProductReviewList(MarketReview review)
    {
        return baseMapper.selectProductReviewList(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitReviews(Long userId, MarketReviewSubmitBo bo)
    {
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        if (bo == null || bo.getReviews() == null || bo.getReviews().isEmpty())
        {
            throw new ServiceException("请选择要评价的商品");
        }

        Date now = DateUtils.getNowDate();
        int created = 0;
        for (MarketReviewSubmitBo.ReviewItem item : bo.getReviews())
        {
            if (item == null || item.getOrderItemId() == null)
            {
                continue;
            }
            MarketOrderItem orderItem = orderItemService.getById(item.getOrderItemId());
            if (orderItem == null)
            {
                throw new ServiceException("订单明细不存在");
            }
            // 归属校验：仅本人订单可评价
            MarketOrder order = orderService.getOwnOrderOrNull(userId, orderItem.getOrderId());
            if (order == null)
            {
                throw new ServiceException("无权评价该订单");
            }
            if (!STATUS_FINISHED.equals(order.getStatus()))
            {
                throw new ServiceException("仅已完成的订单可评价");
            }
            // 去重：同一订单明细仅可评价一次
            Long existing = this.lambdaQuery()
                    .eq(MarketReview::getOrderItemId, item.getOrderItemId())
                    .count();
            if (existing != null && existing > 0)
            {
                continue;
            }

            MarketReview review = new MarketReview();
            review.setOrderId(order.getOrderId());
            review.setOrderItemId(item.getOrderItemId());
            review.setUserId(order.getUserId());
            review.setProductId(orderItem.getProductId());
            review.setShopId(order.getShopId());
            Integer rating = item.getRating();
            review.setRating(rating == null || rating < 1 ? 5 : Math.min(rating, 5));
            review.setContent(item.getContent());
            review.setCreateTime(now);
            this.save(review);
            created++;
        }
        return created;
    }
}
