package com.luojia.soundscape.order.client;

import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.order.OrderInfo;
import com.luojia.soundscape.order.client.impl.OrderDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 订单模块远程调用API接口
 * </p>
 *
 * @author Luojia Soundscape Contributors
 */
@FeignClient(value = "luojia-soundscape-order",path = "api/order",fallback = OrderDegradeFeignClient.class)
public interface OrderFeignClient {


    /**
     * 根据订单编号查询订单信息（包含订单明细）
     * @param orderNo
     * @return
     */
    @GetMapping("/orderInfo/getOrderInfo/{orderNo}")
    public Result<OrderInfo> getOrderInfo(@PathVariable String orderNo);

    @GetMapping("/orderInfo/orderPaySuccess/{orderNo}")
    Result orderPaySuccess(@PathVariable String orderNo);

}
