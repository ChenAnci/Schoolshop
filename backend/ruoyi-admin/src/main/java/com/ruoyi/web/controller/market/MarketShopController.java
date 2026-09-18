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
import com.ruoyi.market.domain.MarketShop;
import com.ruoyi.market.service.IMarketShopService;

/**
 * 店铺信息 操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/market/shop")
public class MarketShopController extends BaseController
{
    @Autowired
    private IMarketShopService shopService;

    /**
     * 查询店铺信息列表
     */
    @PreAuthorize("@ss.hasPermi('market:shop:list')")
    @GetMapping("/list")
    public TableDataInfo list(MarketShop shop)
    {
        startPage();
        List<MarketShop> list = shopService.selectShopList(shop);
        return getDataTable(list);
    }

    /**
     * 获取店铺信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('market:shop:query')")
    @GetMapping(value = "/{shopId}")
    public AjaxResult getInfo(@PathVariable Long shopId)
    {
        return success(shopService.getById(shopId));
    }

    /**
     * 新增店铺信息
     */
    @PreAuthorize("@ss.hasPermi('market:shop:add')")
    @Log(title = "店铺信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MarketShop shop)
    {
        shop.setCreateBy(getUsername());
        shop.setCreateTime(DateUtils.getNowDate());
        return toAjax(shopService.save(shop));
    }

    /**
     * 修改店铺信息
     */
    @PreAuthorize("@ss.hasPermi('market:shop:edit')")
    @Log(title = "店铺信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody MarketShop shop)
    {
        shop.setUpdateBy(getUsername());
        shop.setUpdateTime(DateUtils.getNowDate());
        return toAjax(shopService.updateById(shop));
    }

    /**
     * 删除店铺信息
     */
    @PreAuthorize("@ss.hasPermi('market:shop:remove')")
    @Log(title = "店铺信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{shopIds}")
    public AjaxResult remove(@PathVariable Long[] shopIds)
    {
        return toAjax(shopService.removeByIds(Arrays.asList(shopIds)));
    }

    /**
     * 获取当前商家的店铺信息（商家作用域）
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @GetMapping("/my")
    public AjaxResult getMyShop()
    {
        MarketShop shop = shopService.lambdaQuery()
                .eq(MarketShop::getUserId, getUserId())
                .one();
        return success(shop);
    }

    /**
     * 修改当前商家的店铺信息（商家作用域，仅能修改自己店铺）
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @Log(title = "我的店铺", businessType = BusinessType.UPDATE)
    @PutMapping("/my")
    public AjaxResult editMyShop(@RequestBody MarketShop shop)
    {
        MarketShop own = shopService.lambdaQuery()
                .eq(MarketShop::getUserId, getUserId())
                .one();
        if (own == null)
        {
            return error("尚未开通店铺，无法编辑");
        }
        // 强制限定为本人店铺，防止越权修改他人店铺
        shop.setShopId(own.getShopId());
        shop.setUserId(own.getUserId());
        shop.setCreateBy(null);
        shop.setCreateTime(null);
        shop.setUpdateBy(getUsername());
        shop.setUpdateTime(DateUtils.getNowDate());
        return toAjax(shopService.updateById(shop));
    }
}