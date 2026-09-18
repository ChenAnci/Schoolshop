package com.ruoyi.web.controller.market;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysUserService;

/**
 * 商家入驻申请（当前登录用户提交申请 / 查询申请状态）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/market/merchant")
public class MarketMerchantApplyController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    /**
     * 查询当前登录用户的商家入驻申请状态
     */
    @GetMapping("/apply/status")
    public AjaxResult myApplyStatus()
    {
        SysUser cur = userService.selectUserById(getUserId());
        if (cur == null)
        {
            return error("用户不存在");
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("merchantApplyStatus", cur.getMerchantApplyStatus());
        ajax.put("merchantApplyRemark", cur.getMerchantApplyRemark());
        return ajax;
    }

    /**
     * 提交商家入驻申请（仅 0未申请 / 3已驳回 状态可再次申请）
     */
    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody(required = false) SysUser body)
    {
        Long userId = getUserId();
        SysUser cur = userService.selectUserById(userId);
        if (cur == null)
        {
            return error("用户不存在");
        }
        String st = cur.getMerchantApplyStatus();
        if (StringUtils.isNotNull(st) && !"0".equals(st) && !"3".equals(st))
        {
            return error("当前状态不可重复申请");
        }
        String remark = (body == null) ? null : body.getRemark();
        if (StringUtils.isEmpty(remark))
        {
            remark = "用户申请入驻商家";
        }
        SysUser upd = new SysUser();
        upd.setUserId(userId);
        upd.setMerchantApplyStatus("1");
        upd.setMerchantApplyRemark(remark);
        return toAjax(userService.updateMerchantApply(upd));
    }
}