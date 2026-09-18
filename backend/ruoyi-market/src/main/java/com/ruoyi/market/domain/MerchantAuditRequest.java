package com.ruoyi.market.domain;

/**
 * 商家入驻审核请求体
 *
 * @author ruoyi
 */
public class MerchantAuditRequest
{
    /** 被审核用户ID */
    private Long userId;

    /** 审核动作：approve 通过 / reject 驳回 */
    private String action;

    /** 审核意见（驳回必填） */
    private String remark;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}