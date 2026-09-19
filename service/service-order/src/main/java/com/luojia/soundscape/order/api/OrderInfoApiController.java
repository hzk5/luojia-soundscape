package com.luojia.soundscape.order.api;

import com.luojia.soundscape.common.login.SoundscapeLogin;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.common.util.AuthContextHolder;
import com.luojia.soundscape.model.order.OrderInfo;
import com.luojia.soundscape.order.service.OrderInfoService;
import com.luojia.soundscape.vo.order.OrderInfoVo;
import com.luojia.soundscape.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

    @Autowired
    private OrderInfoService orderInfoService;


    /**
     * 订单结算（对三种不同商品类型进行结算，渲染结算页）
     *
     * @param tradeVo 包含：购买项目类型、购买项目ID、声音数量
     * @return 订单VO
     */
    @SoundscapeLogin
    @Operation(summary = "订单结算（对三种不同商品类型进行结算，渲染结算页）")
    @PostMapping("/orderInfo/trade")
    public Result<OrderInfoVo> trade(@RequestBody TradeVo tradeVo) {
        OrderInfoVo orderInfoVo = orderInfoService.trade(tradeVo);
        return Result.ok(orderInfoVo);
    }


    /**
     * 提交订单（选择支付方式 支持余额跟微信支付）
     * @param orderInfoVo 订单VO信息
     * @return {orderNo: 订单编号} 前端获取到订单编号后可以跳转到微信支付页面、或跳转支付成功页面
     */
    @SoundscapeLogin
    @Operation(summary = "提交订单（选择支付方式）")
    @PostMapping("/orderInfo/submitOrder")
    public Result<Map<String, String>> submitOrder(@RequestBody OrderInfoVo orderInfoVo){
        //1. 获取用户ID
        Long userId = AuthContextHolder.getUserId();
        //2. 调用业务层
        Map<String, String> map = orderInfoService.submitOrder(userId, orderInfoVo);
        //3.响应结果
        return Result.ok(map);

    }


    /**
     * 根据订单编号查询订单信息（包含订单明细）
     * @param orderNo
     * @return
     */
    @Operation(summary = "根据订单编号查询订单信息（包含订单明细）")
    @GetMapping("/orderInfo/getOrderInfo/{orderNo}")
    public Result<OrderInfo> getOrderInfo(@PathVariable String orderNo){
        OrderInfo orderInfo = orderInfoService.getOrderInfo(orderNo);
        return Result.ok(orderInfo);
    }

    @Operation(summary = "支付成功后更新订单状态并发放权益")
    @GetMapping("/orderInfo/orderPaySuccess/{orderNo}")
    public Result orderPaySuccess(@PathVariable String orderNo) {
        orderInfoService.orderPaySuccess(orderNo);
        return Result.ok();
    }


    /**
     * 分页查询当前用户订单列表（包含订单明细）
     * @param page
     * @param limit
     * @return
     */
    @SoundscapeLogin
    @Operation(summary = "分页查询当前用户订单列表（包含订单明细）")
    @GetMapping("/orderInfo/findUserPage/{page}/{limit}")
    public Result<IPage<OrderInfo>> findUserPage(@PathVariable Long page,@PathVariable Long limit){
        //1. 获取用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.创建分页对象
        IPage<OrderInfo> pageInfo = new Page<>(page, limit);
        //3.调用业务层
        pageInfo  = orderInfoService.findUserPage(pageInfo, userId);
        //4.响应结果
        return Result.ok(pageInfo);
    }
}
