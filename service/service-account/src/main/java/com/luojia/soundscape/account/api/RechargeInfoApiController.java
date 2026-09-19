package com.luojia.soundscape.account.api;

import com.luojia.soundscape.account.service.RechargeInfoService;
import com.luojia.soundscape.common.login.SoundscapeLogin;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.common.util.AuthContextHolder;
import com.luojia.soundscape.model.account.RechargeInfo;
import com.luojia.soundscape.vo.account.RechargeInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class RechargeInfoApiController {

    @Autowired
    private RechargeInfoService rechargeInfoService;


    /**
     * 根据订单编号查询充值记录
     *
     * @param orderNo
     * @return
     */
    @Operation(summary = "根据订单编号查询充值记录")
    @GetMapping("/rechargeInfo/getRechargeInfo/{orderNo}")
    public Result<RechargeInfo> getRechargeInfo(@PathVariable String orderNo) {
        RechargeInfo rechargeInfo = rechargeInfoService.getRechargeInfo(orderNo);
        return Result.ok(rechargeInfo);
    }

    /**
     * 保存充值记录
     *
     * @param rechargeInfoVo
     * @return {“orderNo” :"充值订单编号"}
     */
    @SoundscapeLogin
    @Operation(summary = "保存充值记录")
    @PostMapping("/rechargeInfo/submitRecharge")
    public Result<Map<String, String>> submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        Map<String, String> map = rechargeInfoService.submitRecharge(userId, rechargeInfoVo);
        return Result.ok(map);
    }

    @SoundscapeLogin(required = false)
    @Operation(summary = "支付成功后完成余额充值")
    @GetMapping("/rechargeInfo/rechargePaySuccess/{orderNo}")
    public Result rechargePaySuccess(@PathVariable String orderNo) {
        rechargeInfoService.rechargePaySuccess(orderNo);
        return Result.ok();
    }

}
