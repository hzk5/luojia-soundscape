package com.luojia.soundscape.payment.service;

import java.util.Map;

public interface WxPayService {

    /**
     * 对接微信支付获取小程序拉起微信支付所需参数
     *
     * @param paymentType 支付类型：1301-订单 1302-充值'
     * @param orderNo     订单编号
     * @return {"timeStamp":"","package":"","paySign":"","signType":"RSA","nonceStr":""}
     */
    Map<String, String> createJsapi(String paymentType, String orderNo);

    /**
     * 商户侧主动调用微信查询支付状态
     * @param orderNo 下单/充值 订单编号
     * @return 订单支付状态 true:已支付 false:未支付
     */
    Boolean queryPayStatus(String orderNo);
}
