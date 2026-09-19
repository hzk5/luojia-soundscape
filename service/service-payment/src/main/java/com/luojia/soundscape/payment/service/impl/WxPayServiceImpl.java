package com.luojia.soundscape.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.common.execption.SoundscapeException;
import com.luojia.soundscape.model.payment.PaymentInfo;
import com.luojia.soundscape.payment.config.WxPayV3Config;
import com.luojia.soundscape.payment.service.PaymentInfoService;
import com.luojia.soundscape.payment.service.WxPayService;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.luojia.soundscape.common.constant.SystemConstant.PAYMENT_STATUS_UNPAID;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private WxPayV3Config wxPayV3Config;

    @Autowired(required = false)
    private RSAAutoCertificateConfig rsaAutoCertificateConfig;

    /** 本地默认开启模拟支付，设为 false 才会访问微信支付 API。 */
    @Value("${wechat.v3pay.mock-enabled:true}")
    private boolean mockEnabled;

    /**
     * 对接微信支付获取小程序拉起微信支付所需参数
     *
     * @param paymentType 支付类型：1301-订单 1302-充值'
     * @param orderNo     订单编号
     * @return {"timeStamp":"","package":"","paySign":"","signType":"RSA","nonceStr":""}
     */
    @Override
    public Map<String, String> createJsapi(String paymentType, String orderNo) {
        try {
            //1.保存本地交易记录
            PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo);
            String paymentStatus = paymentInfo.getPaymentStatus();
            if (!PAYMENT_STATUS_UNPAID.equals(paymentStatus)) {
                throw new SoundscapeException(500, "本地交易状态有误");
            }

            if (mockEnabled) {
                log.info("本地模拟微信预下单成功，orderNo={}", orderNo);
                return Map.of("mockPayment", "true", "orderNo", orderNo);
            }

            //2.调用微信支付获取小程序发起支付所需参数
            //2.1 创建调用微信支付JSAPI业务对象
            JsapiServiceExtension jsapiService = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
            PrepayRequest request = new PrepayRequest();
            //付款者信息，要求必须应用下开发者账户，才可以支付 必须使用我的（拉起微信支付页面）
            Payer payer = new Payer();
            payer.setOpenid("odo3j4qp-wC3HVq9Z_D9C0cOr0Zs");
            request.setPayer(payer);
            Amount amount = new Amount();
            amount.setTotal(1); //单位分
            request.setAmount(amount);
            request.setAppid(wxPayV3Config.getAppid());
            request.setMchid(wxPayV3Config.getMerchantId());
            request.setDescription(paymentInfo.getContent());
            request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
            request.setOutTradeNo(paymentInfo.getOrderNo());
            PrepayWithRequestPaymentResponse response = jsapiService.prepayWithRequestPayment(request);
            if (response != null) {
                String timeStamp = response.getTimeStamp();
                String nonceStr = response.getNonceStr();
                String packageVal = response.getPackageVal();
                String signType = response.getSignType();
                String paySign = response.getPaySign();
                Map<String, String> map = new HashMap<>();
                map.put("timeStamp", timeStamp);
                map.put("nonceStr", nonceStr);
                map.put("package", packageVal);
                map.put("signType", signType);
                map.put("paySign", paySign);
                return map;
            }
            return Map.of();
        } catch (SoundscapeException e) {
            log.error("微信支付异常：{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 商户侧主动调用微信查询支付状态
     *
     * @param orderNo 下单/充值 订单编号
     * @return 订单支付状态 true:已支付 false:未支付
     */
    @Override
    public Boolean queryPayStatus(String orderNo) {
        if (mockEnabled) {
            Transaction transaction = new Transaction();
            transaction.setOutTradeNo(orderNo);
            transaction.setTransactionId("MOCK-WX-" + IdUtil.getSnowflakeNextIdStr());
            paymentInfoService.updatePaymentInfo(transaction);
            log.info("本地模拟微信支付成功，orderNo={}", orderNo);
            return true;
        }
        //1.创建调用微信支付JSAPI业务对象
        JsapiServiceExtension jsapiService = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
        //2.创建商户订单查询请求
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(wxPayV3Config.getMerchantId());
        request.setOutTradeNo(orderNo);
        //3.调用微信得到交易对象
        Transaction transaction = jsapiService.queryOrderByOutTradeNo(request);
        //4.判断交易状态
        if (transaction != null) {
            //4.1 查询支付状态
            log.info("订单：{}, 查询支付状态：{}", orderNo, transaction.getTradeState());
            if (transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
                //4.2 判断支付金额跟应付金额是否一致
                Integer payerTotal = transaction.getAmount().getTotal();
                //TODO 根据订单编号查询本地交易记录得到应付金额
                if (payerTotal.intValue() == 1) {
                    return true;
                }
            }
        }
        return false;
    }
}
