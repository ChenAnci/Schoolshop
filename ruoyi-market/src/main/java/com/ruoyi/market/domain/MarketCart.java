package com.ruoyi.market.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 购物车对象 market_cart
 *
 * @author ruoyi
 */
@TableName("market_cart")
public class MarketCart
{
    private static final long serialVersionUID = 1L;

    /** 购物车ID */
    @TableId(value = "cart_id", type = IdType.AUTO)
    private Long cartId;

    /** 用户ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 商品名称（快照） */
    private String productName;

    /** 商品主图（快照） */
    private String productImage;

    /** 单价（快照） */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 是否选中（1选中 0未选中） */
    private String checked;

    /** 加入时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 商品当前状态（关联查询，非表字段） */
    @TableField(exist = false)
    private String productStatus;

    /** 商品所属店铺ID（关联查询，非表字段） */
    @TableField(exist = false)
    private Long shopId;

    /** 商品所属店铺名称（关联查询，非表字段） */
    @TableField(exist = false)
    private String shopName;

    public Long getCartId()
    {
        return cartId;
    }

    public void setCartId(Long cartId)
    {
        this.cartId = cartId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getProductImage()
    {
        return productImage;
    }

    public void setProductImage(String productImage)
    {
        this.productImage = productImage;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public String getChecked()
    {
        return checked;
    }

    public void setChecked(String checked)
    {
        this.checked = checked;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getProductStatus()
    {
        return productStatus;
    }

    public void setProductStatus(String productStatus)
    {
        this.productStatus = productStatus;
    }

    public Long getShopId()
    {
        return shopId;
    }

    public void setShopId(Long shopId)
    {
        this.shopId = shopId;
    }

    public String getShopName()
    {
        return shopName;
    }

    public void setShopName(String shopName)
    {
        this.shopName = shopName;
    }
}