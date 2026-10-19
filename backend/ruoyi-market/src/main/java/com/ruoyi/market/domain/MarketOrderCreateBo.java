package com.ruoyi.market.domain;

import java.util.List;

/**
 * 下单请求参数（C 端结算）
 *
 * 替代原先在 Controller 中手工解析 Map<String, Object> 的写法：
 * 由 SpringMVC 直接绑定并做类型转换，Controller 不再出现 toLongList/str 之类的解析代码。
 *
 * 请求体示例：
 * {"cartIds":[1,2], "receiverName":"张三", "receiverPhone":"138...", "receiverAddress":"三食堂门口", "remark":"不要辣"}
 *
 * @author ruoyi
 */
public class MarketOrderCreateBo
{
    /** 待结算的购物车条目ID */
    private List<Long> cartIds;

    /** 取货人姓名 */
    private String receiverName;

    /** 取货人电话 */
    private String receiverPhone;

    /** 取货地址 */
    private String receiverAddress;

    /** 订单备注 */
    private String remark;

    public List<Long> getCartIds()
    {
        return cartIds;
    }

    public void setCartIds(List<Long> cartIds)
    {
        this.cartIds = cartIds;
    }

    public String getReceiverName()
    {
        return receiverName;
    }

    public void setReceiverName(String receiverName)
    {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone()
    {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone)
    {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverAddress()
    {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress)
    {
        this.receiverAddress = receiverAddress;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
