package com.luojia.soundscape.account.impl;


import com.luojia.soundscape.account.AccountFeignClient;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.account.RechargeInfo;
import com.luojia.soundscape.vo.account.AccountDeductVo;
import org.springframework.stereotype.Component;

@Component
public class AccountDegradeFeignClient implements AccountFeignClient {

    @Override
    public Result checkAndDeduct(AccountDeductVo accountDeductVo) {
        return null;
    }

    @Override
    public Result<RechargeInfo> getRechargeInfo(String orderNo) {
        return null;
    }

    @Override
    public Result rechargePaySuccess(String orderNo) {
        return Result.fail();
    }
}
