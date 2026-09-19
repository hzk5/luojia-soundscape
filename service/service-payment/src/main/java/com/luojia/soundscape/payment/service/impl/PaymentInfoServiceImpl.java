package com.luojia.soundscape.payment.service.impl;

import cn.hutool.core.lang.Assert;
import com.luojia.soundscape.account.AccountFeignClient;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.common.execption.SoundscapeException;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.account.RechargeInfo;
import com.luojia.soundscape.model.order.OrderInfo;
import com.luojia.soundscape.model.payment.PaymentInfo;
import com.luojia.soundscape.order.client.OrderFeignClient;
import com.luojia.soundscape.payment.mapper.PaymentInfoMapper;
import com.luojia.soundscape.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static com.luojia.soundscape.common.constant.SystemConstant.*;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private AccountFeignClient accountFeignClient;

    /**
     * 保存本地交易记录用于对账
     *
     * @param paymentType 支付类型：1301-订单 1302-充值
     * @param orderNo     订单编号
     * @return 本地交易记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentInfo savePaymentInfo(String paymentType, String orderNo) {
        //1. 根据订单编号查询本地交易记录，存在则返回即可
        PaymentInfo paymentInfo = this.getOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
        if (paymentInfo != null) {
            return paymentInfo;
        }
        //2. 构建本地交易记录对象
        paymentInfo = new PaymentInfo();
        //3. 处理支付类型为订单 封装交易金额、内容、用户ID
        if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)) {
            //3.1 远程调用"订单服务"获取订单信息
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderNo).getData();
            Assert.notNull(orderInfo, "订单不存在");

            //3.2 判断订单支付状态：必须是未支付
            String orderStatus = orderInfo.getOrderStatus();
            if (ORDER_STATUS_PAID.equals(orderStatus) || ORDER_STATUS_CANCEL.equals(orderStatus)) {
                throw new SoundscapeException(500, "订单状态有误");
            }
            //3.3 封装交易金额、内容、用户ID
            paymentInfo.setAmount(orderInfo.getOrderAmount());
            paymentInfo.setUserId(orderInfo.getUserId());
            paymentInfo.setContent(orderInfo.getOrderTitle());
        }
        //4. 处理支付类型为充值  封装交易金额、内容、用户ID
        if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)) {
            //3.1 远程调用"订单服务"获取订单信息
            RechargeInfo rechargeInfo = accountFeignClient.getRechargeInfo(orderNo).getData();
            Assert.notNull(rechargeInfo, "充值不存在");

            //3.2 判断订单支付状态：必须是未支付
            String rechargeStatus = rechargeInfo.getRechargeStatus();
            if (ORDER_STATUS_PAID.equals(rechargeStatus) || ORDER_STATUS_CANCEL.equals(rechargeStatus)) {
                throw new SoundscapeException(500, "充值状态有误");
            }
            //3.3 封装交易金额、内容、用户ID
            paymentInfo.setAmount(rechargeInfo.getRechargeAmount());
            paymentInfo.setUserId(rechargeInfo.getUserId());
            paymentInfo.setContent("充值" + rechargeInfo.getRechargeAmount() + "元");
        }
        //5.保存本地交易记录
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPayWay(ORDER_PAY_WAY_WEIXIN);
        paymentInfo.setPaymentStatus(PAYMENT_STATUS_UNPAID);
        //6.返回本地交易记录
        this.save(paymentInfo);
        //7.TODO 自动取消未支付本地交易记录
        return paymentInfo;
    }

    /**
     * 支付成功后更新本地交易记录，并通知订单服务发放权益。
     * 条件更新保证同一订单重复查询时只处理一次。
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void updatePaymentInfo(Transaction transaction) {
        String orderNo = transaction.getOutTradeNo();
        PaymentInfo paymentInfo = this.getOne(
                new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo)
        );
        Assert.notNull(paymentInfo, "交易记录不存在");
        if (!PAYMENT_STATUS_UNPAID.equals(paymentInfo.getPaymentStatus())) {
            return;
        }

        boolean updated = this.update(
                new LambdaUpdateWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getId, paymentInfo.getId())
                        .eq(PaymentInfo::getPaymentStatus, PAYMENT_STATUS_UNPAID)
                        .set(PaymentInfo::getPaymentStatus, PAYMENT_STATUS_PAID)
                        .set(PaymentInfo::getOutTradeNo, transaction.getTransactionId())
                        .set(PaymentInfo::getCallbackTime, new Date())
                        .set(PaymentInfo::getCallbackContent, transaction.toString())
        );
        if (!updated) {
            return;
        }

        if (PAYMENT_TYPE_ORDER.equals(paymentInfo.getPaymentType())) {
            Result result = orderFeignClient.orderPaySuccess(orderNo);
            if (result == null || result.getCode().intValue() != 200) {
                throw new SoundscapeException(500, "订单支付成功业务处理失败");
            }
            return;
        }
        if (PAYMENT_TYPE_RECHARGE.equals(paymentInfo.getPaymentType())) {
            Result result = accountFeignClient.rechargePaySuccess(orderNo);
            if (result == null || result.getCode().intValue() != 200) {
                throw new SoundscapeException(500, "充值支付成功业务处理失败");
            }
            return;
        }
        throw new SoundscapeException(500, "未知的支付类型");
    }
}
