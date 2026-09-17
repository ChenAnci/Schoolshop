package com.ruoyi.web.controller.market;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.market.domain.MarketCategory;
import com.ruoyi.market.service.IMarketCategoryService;

/**
 * 商品分类 信息操作处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/market/category")
public class MarketCategoryController extends BaseController
{
    @Autowired
    private IMarketCategoryService categoryService;

    /**
     * 查询商品分类列表
     */
    @PreAuthorize("@ss.hasPermi('market:category:list')")
    @GetMapping("/list")
    public TableDataInfo list(MarketCategory category)
    {
        startPage();
        List<MarketCategory> list = categoryService.selectCategoryList(category);
        return getDataTable(list);
    }

    /**
     * 获取商品分类详细信息
     */
    @PreAuthorize("@ss.hasPermi('market:category:query')")
    @GetMapping(value = "/{categoryId}")
    public AjaxResult getInfo(@PathVariable Long categoryId)
    {
        return success(categoryService.getById(categoryId));
    }

    /**
     * 新增商品分类
     */
    @PreAuthorize("@ss.hasPermi('market:category:add')")
    @Log(title = "商品分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MarketCategory category)
    {
        category.setCreateBy(getUsername());
        category.setCreateTime(DateUtils.getNowDate());
        return toAjax(categoryService.save(category));
    }

    /**
     * 修改商品分类
     */
    @PreAuthorize("@ss.hasPermi('market:category:edit')")
    @Log(title = "商品分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody MarketCategory category)
    {
        category.setUpdateBy(getUsername());
        category.setUpdateTime(DateUtils.getNowDate());
        return toAjax(categoryService.updateById(category));
    }

    /**
     * 删除商品分类
     */
    @PreAuthorize("@ss.hasPermi('market:category:remove')")
    @Log(title = "商品分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds)
    {
        return toAjax(categoryService.removeByIds(Arrays.asList(categoryIds)));
    }
}
