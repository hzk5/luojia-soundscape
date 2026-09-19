package com.luojia.soundscape.payment.service;

import com.luojia.soundscape.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wechat.pay.java.service.payments.model.Transaction;

public interface PaymentInfoService extends IService<PaymentInfo> {

    /**
     * 保存本地交易记录用于对账
     * @param paymentType 支付类型：1301-订单 1302-充值
     * @param orderNo 订单编号
     * @return 本地交易记录
     */
    PaymentInfo savePaymentInfo(String paymentType, String orderNo);

    /** 本地模拟或真实微信支付成功后的统一业务入口。 */
    void updatePaymentInfo(Transaction transaction);
}
