package com.ruoyi.market.domain;

import java.util.List;

/**
 * 提交评价请求参数（C 端）
 *
 * 请求体示例：
 * {"reviews":[{"orderItemId":1,"rating":5,"content":"东西很好"}]}
 *
 * @author ruoyi
 */
public class MarketReviewSubmitBo
{
    /** 待评价的订单明细列表 */
    private List<ReviewItem> reviews;

    public List<ReviewItem> getReviews()
    {
        return reviews;
    }

    public void setReviews(List<ReviewItem> reviews)
    {
        this.reviews = reviews;
    }

    /**
     * 单条评价入参
     */
    public static class ReviewItem
    {
        /** 订单明细ID（一条明细仅可评价一次） */
        private Long orderItemId;

        /** 评分 1-5 星 */
        private Integer rating;

        /** 评价内容 */
        private String content;

        public Long getOrderItemId()
        {
            return orderItemId;
        }

        public void setOrderItemId(Long orderItemId)
        {
            this.orderItemId = orderItemId;
        }

        public Integer getRating()
        {
            return rating;
        }

        public void setRating(Integer rating)
        {
            this.rating = rating;
        }

        public String getContent()
        {
            return content;
        }

        public void setContent(String content)
        {
            this.content = content;
        }
    }
}
