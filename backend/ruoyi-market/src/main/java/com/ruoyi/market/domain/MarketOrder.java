package com.ruoyi.market.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 订单对象 market_orders
 *
 * @author ruoyi
 */
@TableName("market_orders")
public class MarketOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;

    /** 订单编号 */
    private String orderNo;

    /** 下单用户ID */
    private Long userId;

    /** 店铺ID */
    private Long shopId;

    /** 订单总金额（元） */
    private BigDecimal totalAmount;

    /** 实付金额（元） */
    private BigDecimal payAmount;

    /** 状态（0待商家接单 1待自提 2已完成 3已取消 4审查中 5审查完成） */
    private String status;

    /** 审查位（0未审查 1审查中 2已仲裁） */
    private String auditFlag;

    /** 审查/仲裁意见 */
    private String auditRemark;

    /** 取货人姓名 */
    private String receiverName;

    /** 取货人电话 */
    private String receiverPhone;

    /** 取货地址 */
    private String receiverAddress;

    /** 支付时间 */
    private Date payTime;

    /** 取货时间 */
    private Date takeTime;

    /** 完成时间 */
    private Date finishTime;

    /** 取消时间 */
    private Date cancelTime;

    /** 下单用户名（关联查询，非表字段） */
    @TableField(exist = false)
    private String userName;

    /** 店铺名称（关联查询，非表字段） */
    @TableField(exist = false)
    private String shopName;

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getShopId()
    {
        return shopId;
    }

    public void setShopId(Long shopId)
    {
        this.shopId = shopId;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPayAmount()
    {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount)
    {
        this.payAmount = payAmount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getReceiverName()
    {
        return receiverName;
    }

    public void setReceiverName(String receiverName)
    {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone()
    {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone)
    {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverAddress()
    {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress)
    {
        this.receiverAddress = receiverAddress;
    }

    public Date getPayTime()
    {
        return payTime;
    }

    public void setPayTime(Date payTime)
    {
        this.payTime = payTime;
    }

    public Date getTakeTime()
    {
        return takeTime;
    }

    public void setTakeTime(Date takeTime)
    {
        this.takeTime = takeTime;
    }

    public Date getFinishTime()
    {
        return finishTime;
    }

    public void setFinishTime(Date finishTime)
    {
        this.finishTime = finishTime;
    }

    public Date getCancelTime()
    {
        return cancelTime;
    }

    public void setCancelTime(Date cancelTime)
    {
        this.cancelTime = cancelTime;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getShopName()
    {
        return shopName;
    }

    public void setShopName(String shopName)
    {
        this.shopName = shopName;
    }

    public String getAuditFlag()
    {
        return auditFlag;
    }

    public void setAuditFlag(String auditFlag)
    {
        this.auditFlag = auditFlag;
    }

    public String getAuditRemark()
    {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark)
    {
        this.auditRemark = auditRemark;
    }
}