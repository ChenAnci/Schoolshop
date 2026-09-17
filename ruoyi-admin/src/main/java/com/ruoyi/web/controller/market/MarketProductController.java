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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.market.domain.MarketProduct;
import com.ruoyi.market.service.IMarketProductService;

/**
 * 商品信息 操作处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/market/product")
public class MarketProductController extends BaseController
{
    @Autowired
    private IMarketProductService productService;

    /**
     * 查询商品信息列表
     */
    @PreAuthorize("@ss.hasPermi('market:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(MarketProduct product)
    {
        startPage();
        List<MarketProduct> list = productService.selectProductList(product);
        return getDataTable(list);
    }

    /**
     * 获取商品信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('market:product:query')")
    @GetMapping(value = "/{productId}")
    public AjaxResult getInfo(@PathVariable Long productId)
    {
        return success(productService.getById(productId));
    }

    /**
     * 新增商品信息
     */
    @PreAuthorize("@ss.hasPermi('market:product:add')")
    @Log(title = "商品信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MarketProduct product)
    {
        product.setCreateBy(getUsername());
        product.setCreateTime(DateUtils.getNowDate());
        return toAjax(productService.save(product));
    }

    /**
     * 修改商品信息
     */
    @PreAuthorize("@ss.hasPermi('market:product:edit')")
    @Log(title = "商品信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody MarketProduct product)
    {
        product.setUpdateBy(getUsername());
        product.setUpdateTime(DateUtils.getNowDate());
        return toAjax(productService.updateById(product));
    }

    /**
     * 上下架商品（管理端事后管控）：status 0上架 1下架
     */
    @PreAuthorize("@ss.hasPermi('market:product:edit')")
    @Log(title = "商品信息", businessType = BusinessType.UPDATE)
    @PutMapping("/{productId}/status")
    public AjaxResult changeStatus(@PathVariable Long productId, @RequestParam String status)
    {
        if (!"0".equals(status) && !"1".equals(status))
        {
            return error("状态参数不合法");
        }
        MarketProduct product = productService.getById(productId);
        if (product == null)
        {
            return error("商品不存在");
        }
        product.setStatus(status);
        product.setUpdateBy(getUsername());
        product.setUpdateTime(DateUtils.getNowDate());
        return toAjax(productService.updateById(product));
    }

    /**
     * 删除商品信息
     */
    @PreAuthorize("@ss.hasPermi('market:product:remove')")
    @Log(title = "商品信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds)
    {
        return toAjax(productService.removeByIds(Arrays.asList(productIds)));
    }
}
