package com.ruoyi.market.domain;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品信息对象 market_product
 * 
 * @author ruoyi
 */
@TableName("market_product")
public class MarketProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 商品ID */
    @TableId(value = "product_id", type = IdType.AUTO)
    private Long productId;

    /** 所属店铺ID */
    private Long shopId;

    /** 所属分类ID */
    private Long categoryId;

    /** 商品名称 */
    private String productName;

    /** 商品描述 */
    private String productDesc;

    /** 商品主图 */
    private String productImage;

    /** 价格（元） */
    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 销量 */
    private Integer sales;

    /** 状态（0上架 1下架） */
    private String status;

    /** 显示顺序 */
    private Integer sort;

    /** 分类名称（关联查询，非表字段） */
    @TableField(exist = false)
    private String categoryName;

    /** 店铺名称（关联查询，非表字段） */
    @TableField(exist = false)
    private String shopName;

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getShopId()
    {
        return shopId;
    }

    public void setShopId(Long shopId)
    {
        this.shopId = shopId;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getProductDesc()
    {
        return productDesc;
    }

    public void setProductDesc(String productDesc)
    {
        this.productDesc = productDesc;
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

    public Integer getStock()
    {
        return stock;
    }

    public void setStock(Integer stock)
    {
        this.stock = stock;
    }

    public Integer getSales()
    {
        return sales;
    }

    public void setSales(Integer sales)
    {
        this.sales = sales;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
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
