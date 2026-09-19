package com.luojia.soundscape.payment.api;

import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;

    /**
     * 对接微信支付获取小程序拉起微信支付所需参数
     *
     * @param paymentType 支付类型：1301-订单 1302-充值'
     * @param orderNo     订单编号
     * @return {"timeStamp":"","package":"","paySign":"","signType":"RSA","nonceStr":""}
     */
    @Operation(summary = "对接微信支付获取小程序拉起微信支付所需参数")
    @PostMapping("/wxPay/createJsapi/{paymentType}/{orderNo}")
    public Result<Map<String, String>> createJsapi(@PathVariable String paymentType, @PathVariable String orderNo) {
        Map<String, String> map = wxPayService.createJsapi(paymentType, orderNo);
        return Result.ok(map);
    }

    /**
     * 商户侧主动调用微信查询支付状态
     * @param orderNo 下单/充值 订单编号
     * @return 订单支付状态 true:已支付 false:未支付
     */
    @GetMapping("/wxPay/queryPayStatus/{orderNo}")
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo){
        Boolean flag = wxPayService.queryPayStatus(orderNo);
        return Result.ok(flag);
    }

}
