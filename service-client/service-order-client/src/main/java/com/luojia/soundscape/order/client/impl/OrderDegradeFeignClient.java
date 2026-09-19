package com.luojia.soundscape.order.client.impl;


import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.order.OrderInfo;
import com.luojia.soundscape.order.client.OrderFeignClient;
import org.springframework.stereotype.Component;

@Component
public class OrderDegradeFeignClient implements OrderFeignClient {

    @Override
    public Result<OrderInfo> getOrderInfo(String orderNo) {
        return null;
    }

    @Override
    public Result orderPaySuccess(String orderNo) {
        return Result.fail();
    }
}
