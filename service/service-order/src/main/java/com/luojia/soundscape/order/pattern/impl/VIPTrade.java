package com.luojia.soundscape.order.pattern.impl;

import cn.hutool.core.lang.Assert;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.model.user.VipServiceConfig;
import com.luojia.soundscape.order.pattern.AbstractTradeStrategy;
import com.luojia.soundscape.user.client.UserFeignClient;
import com.luojia.soundscape.vo.order.OrderDerateVo;
import com.luojia.soundscape.vo.order.OrderDetailVo;
import com.luojia.soundscape.vo.order.OrderInfoVo;
import com.luojia.soundscape.vo.order.TradeVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_VIP)
public class VIPTrade extends AbstractTradeStrategy {


    @Autowired
    private UserFeignClient userFeignClient;


    /**
     * 对VIP商品类型进行结算
     * @param tradeVo
     * @param userId
     * @return
     */
    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        //1.创建订单VO对象，创建初始三个金额，两个集合（商品、优惠）
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        //1.1 初始化原价金额 TODO 必须是BigDecimal类型且必须是字符串
        BigDecimal originalAmount = new BigDecimal("0.00");
        //1.2 初始化订单金额
        BigDecimal orderAmount = new BigDecimal("0.00");
        //1.3 初始化减免金额
        BigDecimal derateAmount = new BigDecimal("0.00");

        //1.4. 初始化商品列表集合
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        //1.5. 初始化商品优惠集合
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();

        //2.1 远程调用用户服务得到VIP套餐信息
        VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(tradeVo.getItemId()).getData();
        Assert.notNull(vipServiceConfig, "VIP套餐{}不存在", tradeVo.getItemId());
        //2.2 计算原金额、订单金额、减免金额
        originalAmount = vipServiceConfig.getPrice();
        orderAmount = vipServiceConfig.getDiscountPrice();
        //2.3 封装商品信息列表
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setItemId(tradeVo.getItemId());
        orderDetailVo.setItemName("VIP套餐：" + vipServiceConfig.getName());
        orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
        orderDetailVo.setItemPrice(originalAmount);
        orderDetailVoList.add(orderDetailVo);

        //2.4 如果存在优惠，封装优惠列表
        if (originalAmount.compareTo(orderAmount) == 1) {
            derateAmount = originalAmount.subtract(orderAmount);
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("VIP套餐限时优惠:" + derateAmount);
            orderDerateVoList.add(orderDerateVo);
        }

        //5.封装订单VO对象
        //5.1 封装相关价格信息
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        //5.2 封装商品相关集合
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        //5.3  封装其他信息：流水号、时间戳及签名、项目类型等
        //5.3.1 购买项目类型
        orderInfoVo.setItemType(tradeVo.getItemType());
        //5.3.2 流水号机制，防止订单重复提交（1.用户网络卡连续点击结算按钮2.成功提交订单，回退到订单确认页再次提交）
        generateTradeNo(orderInfoVo, userId);

        //5.3.3 签名机制，时间戳+签名值，防止订单结算页数据被篡改
        generateSign(orderInfoVo, userId);
        return orderInfoVo;
    }
}
