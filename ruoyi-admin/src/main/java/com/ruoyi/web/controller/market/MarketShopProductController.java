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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.market.domain.MarketProduct;
import com.ruoyi.market.domain.MarketShop;
import com.ruoyi.market.service.IMarketProductService;
import com.ruoyi.market.service.IMarketShopService;

/**
 * 商家端商品管理（商家作用域）
 *
 * 商家端（8082）使用，全部接口基于当前登录商家所属店铺隔离（getUserId → shop_id），
 * 仅能操作自己店铺的商品，防止越权操作其他商家的商品。
 *
 * 商品状态（status）：0上架 1下架；发布即上架（无审核流程），下架商品对 C 端不可见。
 *
 * 接口路径：/market/shop/product/**
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/market/shop/product")
public class MarketShopProductController extends BaseController
{
    @Autowired
    private IMarketShopService shopService;

    @Autowired
    private IMarketProductService productService;

    /** 获取当前商家自己的店铺，未开通返回 null */
    private MarketShop getMyShop()
    {
        return shopService.lambdaQuery()
                .eq(MarketShop::getUserId, getUserId())
                .one();
    }

    /** 校验商品归属当前商家店铺，不匹配返回 null */
    private MarketProduct getOwnProduct(Long productId, Long shopId)
    {
        return productService.lambdaQuery()
                .eq(MarketProduct::getProductId, productId)
                .eq(MarketProduct::getShopId, shopId)
                .one();
    }

    /**
     * 获取当前商家店铺的商品列表（可按名称/状态筛选，含分类名/店铺名）
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @GetMapping("/list")
    public TableDataInfo list(MarketProduct product)
    {
        MarketShop shop = getMyShop();
        if (shop == null)
        {
            return getDataTable(List.of());
        }
        product.setShopId(shop.getShopId());
        // 商家端展示全部状态（含下架），列表页根据状态筛选
        startPage();
        List<MarketProduct> list = productService.selectProductList(product);
        return getDataTable(list);
    }

    /**
     * 获取商品详情（校验归属）
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @GetMapping("/{productId}")
    public AjaxResult getInfo(@PathVariable Long productId)
    {
        MarketShop shop = getMyShop();
        if (shop == null)
        {
            return error("尚未开通店铺");
        }
        MarketProduct product = getOwnProduct(productId, shop.getShopId());
        if (product == null)
        {
            return error("商品不存在");
        }
        return success(product);
    }

    /**
     * 发布商品（自动归属当前商家店铺，默认上架）
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @Log(title = "商家商品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MarketProduct product)
    {
        MarketShop shop = getMyShop();
        if (shop == null)
        {
            return error("尚未开通店铺，无法发布商品");
        }
        if (StringUtils.isEmpty(product.getProductName()))
        {
            return error("商品名称不能为空");
        }
        if (product.getCategoryId() == null)
        {
            return error("请选择商品分类");
        }
        if (product.getPrice() == null || product.getPrice().signum() < 0)
        {
            return error("价格不能为负");
        }
        product.setProductId(null);
        product.setShopId(shop.getShopId());
        product.setStatus(StringUtils.isBlank(product.getStatus()) ? "0" : product.getStatus());
        if (product.getSales() == null)
        {
            product.setSales(0);
        }
        if (product.getStock() == null)
        {
            product.setStock(0);
        }
        product.setCreateBy(getUsername());
        product.setCreateTime(DateUtils.getNowDate());
        return toAjax(productService.save(product));
    }

    /**
     * 编辑商品（校验归属；shopId/销量不允许商家修改）
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @Log(title = "商家商品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody MarketProduct product)
    {
        MarketShop shop = getMyShop();
        if (shop == null)
        {
            return error("尚未开通店铺");
        }
        if (product.getProductId() == null)
        {
            return error("商品ID不能为空");
        }
        MarketProduct exist = getOwnProduct(product.getProductId(), shop.getShopId());
        if (exist == null)
        {
            return error("商品不存在");
        }
        if (StringUtils.isEmpty(product.getProductName()))
        {
            return error("商品名称不能为空");
        }
        // 归属与销量不可被商家篡改
        product.setShopId(shop.getShopId());
        product.setSales(null);
        product.setUpdateBy(getUsername());
        product.setUpdateTime(DateUtils.getNowDate());
        return toAjax(productService.updateById(product));
    }

    /**
     * 上下架商品：status 0上架 1下架
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @Log(title = "商家商品", businessType = BusinessType.UPDATE)
    @PutMapping("/{productId}/status")
    public AjaxResult changeStatus(@PathVariable Long productId, @RequestParam String status)
    {
        if (!"0".equals(status) && !"1".equals(status))
        {
            return error("状态参数不合法");
        }
        MarketShop shop = getMyShop();
        if (shop == null)
        {
            return error("尚未开通店铺");
        }
        MarketProduct exist = getOwnProduct(productId, shop.getShopId());
        if (exist == null)
        {
            return error("商品不存在");
        }
        exist.setStatus(status);
        exist.setUpdateBy(getUsername());
        exist.setUpdateTime(DateUtils.getNowDate());
        return toAjax(productService.updateById(exist));
    }

    /**
     * 删除商品（校验归属）
     */
    @PreAuthorize("@ss.hasRole('merchant')")
    @Log(title = "商家商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds)
    {
        MarketShop shop = getMyShop();
        if (shop == null)
        {
            return error("尚未开通店铺");
        }
        for (Long productId : productIds)
        {
            if (getOwnProduct(productId, shop.getShopId()) == null)
            {
                return error("存在不属于当前店铺的商品，删除失败");
            }
        }
        return toAjax(productService.removeByIds(Arrays.asList(productIds)));
    }
}