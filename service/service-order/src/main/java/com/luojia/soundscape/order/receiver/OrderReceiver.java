package com.luojia.soundscape.order.receiver;

import com.luojia.soundscape.common.rabbit.constant.MqConst;
import com.luojia.soundscape.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@Component
public class OrderReceiver {

    @Autowired
    private OrderInfoService orderInfoService;
    /**
     * 监听延迟关单消息，自动关闭超时订单
     *
     * @param orderId 订单ID
     */
    @SneakyThrows
    @RabbitListener(queues = MqConst.EXCHANGE_CANCEL_ORDER)
    public void cancelOrder(Long orderId, Channel channel, Message message) {
        if (orderId != null) {
            log.info("监听延迟关单消息,订单id：{}", orderId);
            orderInfoService.cancelOrder(orderId);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
