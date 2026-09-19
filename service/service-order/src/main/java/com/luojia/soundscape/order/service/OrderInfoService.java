package com.luojia.soundscape.order.service;

import com.luojia.soundscape.model.order.OrderInfo;
import com.luojia.soundscape.vo.order.OrderInfoVo;
import com.luojia.soundscape.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {


    /**
     * 订单结算（对三种不同商品类型进行结算，渲染结算页）
     *
     * @param tradeVo 包含：购买项目类型、购买项目ID、声音数量
     * @return 订单VO
     */
    OrderInfoVo trade(TradeVo tradeVo);

    /**
     * 提交订单（选择支付方式 支持余额跟微信支付）
     * @param orderInfoVo 订单VO信息
     * @return {orderNo: 订单编号} 前端获取到订单编号后可以跳转到微信支付页面、或跳转支付成功页面
     */
    Map<String, String> submitOrder(Long userId, OrderInfoVo orderInfoVo);

    /**
     * 保存订单相关信息
     * @param userId 用户ID
     * @param orderInfoVo 订单VO
     * @return 订单对象
     */
    OrderInfo saveOrderInfo(Long userId, OrderInfoVo orderInfoVo);

    /**
     * 监听延迟关单消息，自动关闭超时订单
     * @param orderId
     */
    void cancelOrder(Long orderId);

    /**
     * 根据订单编号查询订单信息（包含订单明细）
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfo(String orderNo);

    /**
     * 分页查询指定用户订单列表（包含订单明细）
     * @param pageInfo 分页对象
     * @param userId 用户ID
     * @return 分页对象
     */
    IPage<OrderInfo> findUserPage(IPage<OrderInfo> pageInfo, Long userId);

    /** 支付成功后更新订单状态并发放虚拟权益。 */
    void orderPaySuccess(String orderNo);
}
