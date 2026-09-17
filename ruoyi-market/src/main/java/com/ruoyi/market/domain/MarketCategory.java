package com.ruoyi.market.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品分类对象 market_category
 * 
 * @author ruoyi
 */
@TableName("market_category")
public class MarketCategory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分类ID */
    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;

    /** 父分类ID（0表示顶级） */
    private Long parentId;

    /** 分类名称 */
    private String categoryName;

    /** 显示顺序 */
    private Integer sort;

    /** 状态（0正常 1停用） */
    private String status;

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
