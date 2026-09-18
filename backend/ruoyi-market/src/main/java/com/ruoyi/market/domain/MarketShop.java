package com.ruoyi.market.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 店铺信息对象 market_shop
 *
 * @author ruoyi
 */
@TableName("market_shop")
public class MarketShop extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 店铺ID */
    @TableId(value = "shop_id", type = IdType.AUTO)
    private Long shopId;

    /** 归属商家用户ID */
    private Long userId;

    /** 店铺名称 */
    private String shopName;

    /** 店铺Logo */
    private String shopLogo;

    /** 店铺简介 */
    private String shopDesc;

    /** 联系电话 */
    private String contactPhone;

    /** 状态（0正常 1停用） */
    private String status;

    /** 在售商品数（关联统计，非表字段） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer productCount;

    public Long getShopId()
    {
        return shopId;
    }

    public void setShopId(Long shopId)
    {
        this.shopId = shopId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getShopName()
    {
        return shopName;
    }

    public void setShopName(String shopName)
    {
        this.shopName = shopName;
    }

    public String getShopLogo()
    {
        return shopLogo;
    }

    public void setShopLogo(String shopLogo)
    {
        this.shopLogo = shopLogo;
    }

    public String getShopDesc()
    {
        return shopDesc;
    }

    public void setShopDesc(String shopDesc)
    {
        this.shopDesc = shopDesc;
    }

    public String getContactPhone()
    {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone)
    {
        this.contactPhone = contactPhone;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getProductCount()
    {
        return productCount;
    }

    public void setProductCount(Integer productCount)
    {
        this.productCount = productCount;
    }
}