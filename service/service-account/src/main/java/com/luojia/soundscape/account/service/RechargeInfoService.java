package com.luojia.soundscape.account.service;

import com.luojia.soundscape.model.account.RechargeInfo;
import com.luojia.soundscape.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface RechargeInfoService extends IService<RechargeInfo> {

    /**
     * 根据订单编号查询充值记录
     * @param orderNo
     * @return
     */
    RechargeInfo getRechargeInfo(String orderNo);

    /**
     * 保存充值记录
     *
     * @param rechargeInfoVo
     * @return {“orderNo” :"充值订单编号"}
     */
    Map<String, String> submitRecharge(Long userId, RechargeInfoVo rechargeInfoVo);

    /** 支付成功后修改充值状态、增加余额并记录明细。 */
    void rechargePaySuccess(String orderNo);
}
