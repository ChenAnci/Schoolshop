package com.ruoyi.web.controller.market;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.market.domain.MarketCategory;
import com.ruoyi.market.domain.MarketProduct;
import com.ruoyi.market.domain.MarketReview;
import com.ruoyi.market.domain.MarketShop;
import com.ruoyi.market.service.IMarketCategoryService;
import com.ruoyi.market.service.IMarketProductService;
import com.ruoyi.market.service.IMarketReviewService;
import com.ruoyi.market.service.IMarketShopService;

/**
 * 公开浏览（C 端）操作处理
 *
 * 普通用户（游客/登录用户）浏览店铺与商品使用，不校验管理端权限（无 @PreAuthorize），
 * 仅返回上架商品、正常状态的店铺与分类，实现用户端与管理端的功能隔离。
 */
@RestController
@RequestMapping("/market/browse")
public class MarketBrowseController extends BaseController
{
    @Autowired
    private IMarketProductService productService;

    @Autowired
    private IMarketCategoryService categoryService;

    @Autowired
    private IMarketShopService shopService;

    @Autowired
    private IMarketReviewService reviewService;

    /**
     * 查询正常状态的商品分类列表（C 端）
     */
    @GetMapping("/category/list")
    public AjaxResult categoryList()
    {
        MarketCategory category = new MarketCategory();
        category.setStatus("0");
        List<MarketCategory> list = categoryService.selectCategoryList(category);
        return success(list);
    }

    /**
     * 查询正常状态的店铺列表（C 端，含在售商品数）
     */
    @GetMapping("/shop/list")
    public AjaxResult shopList()
    {
        MarketShop shop = new MarketShop();
        shop.setStatus("0");
        List<MarketShop> list = shopService.selectShopList(shop);
        return success(list);
    }

    /**
     * 分页查询上架商品列表（C 端）
     * 参数：categoryId / shopId / productName 可选，pageNum / pageSize 分页
     */
    @GetMapping("/product/list")
    public TableDataInfo productList(MarketProduct product)
    {
        startPage();
        // 仅展示上架商品，隐藏下架/停用店铺数据
        product.setStatus("0");
        List<MarketProduct> list = productService.selectProductList(product);
        return getDataTable(list);
    }

    /**
     * 查询上架商品详情（C 端，附带店铺信息）
     */
    @GetMapping("/product/{productId}")
    public AjaxResult productInfo(@PathVariable Long productId)
    {
        MarketProduct product = productService.getById(productId);
        if (product == null || !"0".equals(product.getStatus()))
        {
            return error("商品不存在或已下架");
        }
        MarketShop shop = null;
        if (product.getShopId() != null)
        {
            shop = shopService.getById(product.getShopId());
        }
        if (shop == null || !"0".equals(shop.getStatus()))
        {
            return error("商品所属店铺已停用");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("product", product);
        data.put("shop", shop);
        return success(data);
    }

    /**
     * 查询商品评价列表（C 端公开接口，附带评分均值与总数）
     */
    @GetMapping("/product/{productId}/reviews")
    public AjaxResult productReviews(@PathVariable Long productId)
    {
        MarketReview query = new MarketReview();
        query.setProductId(productId);
        List<MarketReview> list = reviewService.selectProductReviewList(query);
        double avg = 0;
        if (!list.isEmpty())
        {
            int sum = 0;
            for (MarketReview r : list)
            {
                sum += r.getRating() == null ? 0 : r.getRating();
            }
            avg = Math.round(sum * 10.0 / list.size()) / 10.0;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("avgRating", avg);
        data.put("count", list.size());
        return success(data);
    }
}