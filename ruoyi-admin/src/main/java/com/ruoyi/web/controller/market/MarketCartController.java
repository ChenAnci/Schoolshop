package com.ruoyi.web.controller.market;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.market.domain.MarketCart;
import com.ruoyi.market.service.IMarketCartService;

/**
 * 购物车 操作处理
 * 说明：购物车为当前登录用户私有数据，仅提供按用户的查询/查看接口（增删改由用户端业务接口完成）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/market/cart")
public class MarketCartController extends BaseController
{
    @Autowired
    private IMarketCartService cartService;

    /**
     * 查询购物车列表
     */
    @PreAuthorize("@ss.hasPermi('market:cart:list')")
    @GetMapping("/list")
    public AjaxResult list(MarketCart cart)
    {
        return success(cartService.selectCartList(cart));
    }

    /**
     * 获取购物车详细信息
     */
    @PreAuthorize("@ss.hasPermi('market:cart:query')")
    @GetMapping(value = "/{cartId}")
    public AjaxResult getInfo(@PathVariable Long cartId)
    {
        return success(cartService.getById(cartId));
    }

    /**
     * 获取当前用户的购物车列表
     */
    @PreAuthorize("@ss.hasPermi('market:cart:list')")
    @GetMapping("/my")
    public AjaxResult my(MarketCart cart)
    {
        cart.setUserId(getUserId());
        return success(cartService.selectCartList(cart));
    }
}