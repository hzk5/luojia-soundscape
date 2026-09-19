package com.luojia.soundscape.order.pattern.impl;

import cn.hutool.core.lang.Assert;
import com.luojia.soundscape.album.AlbumFeignClient;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.model.album.AlbumInfo;
import com.luojia.soundscape.model.album.TrackInfo;
import com.luojia.soundscape.order.pattern.AbstractTradeStrategy;
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
import java.util.stream.Collectors;

/**
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_TRACK)
public class TrackTrade extends AbstractTradeStrategy {

    @Autowired
    private AlbumFeignClient albumFeignClient;

    /**
     * 对声音进行结算
     * @param tradeVo
     * @param userId
     * @return
     */
    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        log.info("对声音进行结算");

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

        //4.处理购买项目类型：声音
        //4.1 远程调用专辑服务，获取未购买声音列表
        Long trackId = tradeVo.getItemId();
        List<TrackInfo> trackInfoList = albumFeignClient.findPaidTrackInfoList(trackId, tradeVo.getTrackCount()).getData();
        Assert.notNull(trackInfoList, "不存在待结算声音", trackId);

        //4.2 远程调用专辑服务获取，专辑价格（声音单价）声音不支持折扣
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(trackInfoList.get(0).getAlbumId()).getData();
        Assert.notNull(albumInfo, "专辑{}不存在", albumInfo.getId());
        BigDecimal price = albumInfo.getPrice();

        //4.3 计算相关价格
        originalAmount = price.multiply(new BigDecimal(trackInfoList.size()));
        orderAmount = originalAmount;

        //4.4 封装订单明细列表
        orderDetailVoList = trackInfoList.stream()
                .map(trackInfo -> {
                    OrderDetailVo orderDetailVo = new OrderDetailVo();
                    orderDetailVo.setItemId(trackInfo.getId());
                    orderDetailVo.setItemName("声音：" + trackInfo.getTrackTitle());
                    orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                    orderDetailVo.setItemPrice(price);
                    return orderDetailVo;
                }).collect(Collectors.toList());

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
