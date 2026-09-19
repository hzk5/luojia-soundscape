package com.luojia.soundscape.order.pattern.impl;

import cn.hutool.core.lang.Assert;
import com.luojia.soundscape.album.AlbumFeignClient;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.model.album.AlbumInfo;
import com.luojia.soundscape.order.pattern.AbstractTradeStrategy;
import com.luojia.soundscape.user.client.UserFeignClient;
import com.luojia.soundscape.vo.order.OrderDerateVo;
import com.luojia.soundscape.vo.order.OrderDetailVo;
import com.luojia.soundscape.vo.order.OrderInfoVo;
import com.luojia.soundscape.vo.order.TradeVo;
import com.luojia.soundscape.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_ALBUM)
public class AlbumTrade extends AbstractTradeStrategy {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    /**
     * 对专辑进行结算
     * @param tradeVo
     * @param userId
     * @return
     */
    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        log.info("对专辑进行结算");
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

        //1.6 获取购买项目类型 付款项目类型: 1001-专辑 1002-声音 1003-vip会员
        String itemType = tradeVo.getItemType();


        //3. 处理购买项目类型：专辑
        //3.1 远程调用用户服务，判断是否重复购买专辑，如果已购过，则业务终止
        Long albumId = tradeVo.getItemId();
        Boolean flag = userFeignClient.isPaidAlbum(albumId).getData();
        Assert.isFalse(flag, "用户已购买专辑{}", albumId);
        //3.2 远程调用专辑服务，获取专辑价格以及折扣（普通用户折扣，VIP会员折扣）
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
        Assert.notNull(albumInfo, "专辑{}不存在", albumId);
        BigDecimal price = albumInfo.getPrice();
        BigDecimal discount = albumInfo.getDiscount();
        BigDecimal vipDiscount = albumInfo.getVipDiscount();

        //3.3 远程调用用户服务，获取用户身份，是否为VIP
        Boolean isVIP = false;
        UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
        Assert.notNull(userInfoVo, "用户{}不存在", userId);
        if (userInfoVo.getIsVip().intValue() == 1
                && userInfoVo.getVipExpireTime().after(new Date())) {
            isVIP = true;
        }

        //3.4 计算相关价格
        //3.4.1 暂时将订单价=原价
        originalAmount = price;
        orderAmount = originalAmount;
        //3.4.2 如果是普通用户，且存在普通用户折扣，则订单价=原价 100 *折扣 6
        if (!isVIP && discount.doubleValue() != -1) {
            orderAmount = originalAmount.multiply(discount)
                    .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
        }
        //3.4.3 如果是VIP会员，且存在VIP用户折扣，则订单价=原价*折扣
        if (isVIP && vipDiscount.doubleValue() != -1) {
            orderAmount = originalAmount.multiply(vipDiscount)
                    .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
        }

        //3.5 封装商品信息列表
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setItemId(albumId);
        orderDetailVo.setItemName("专辑：" + albumInfo.getAlbumTitle());
        orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
        orderDetailVo.setItemPrice(originalAmount);
        orderDetailVoList.add(orderDetailVo);

        //3.6 封装商品优惠列表
        if (originalAmount.compareTo(orderAmount) == 1) {
            derateAmount = originalAmount.subtract(orderAmount);
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("专辑折扣减免：" + derateAmount);
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
