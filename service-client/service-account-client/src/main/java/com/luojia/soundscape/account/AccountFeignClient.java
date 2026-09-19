package com.luojia.soundscape.account;

import com.luojia.soundscape.account.impl.AccountDegradeFeignClient;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.account.RechargeInfo;
import com.luojia.soundscape.vo.account.AccountDeductVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 账号模块远程调用API接口
 * </p>
 *
 * @author Luojia Soundscape Contributors
 */
@FeignClient(value = "luojia-soundscape-account",path = "api/account",fallback = AccountDegradeFeignClient.class)
public interface AccountFeignClient {


    /**
     * 检查且扣减账户金额
     * @param accountDeductVo
     * @return
     */
    @PostMapping("/userAccount/checkAndDeduct")
    public Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo);

    /**
     * 根据订单编号查询充值记录
     * @param orderNo
     * @return
     */
    @GetMapping("/rechargeInfo/getRechargeInfo/{orderNo}")
    public Result<RechargeInfo> getRechargeInfo(@PathVariable String orderNo);

    /** 支付成功后完成充值。 */
    @GetMapping("/rechargeInfo/rechargePaySuccess/{orderNo}")
    Result rechargePaySuccess(@PathVariable String orderNo);
}
