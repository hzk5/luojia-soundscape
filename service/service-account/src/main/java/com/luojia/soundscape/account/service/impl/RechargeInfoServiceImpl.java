package com.luojia.soundscape.account.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.luojia.soundscape.account.mapper.RechargeInfoMapper;
import com.luojia.soundscape.account.service.RechargeInfoService;
import com.luojia.soundscape.account.service.UserAccountService;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.model.account.RechargeInfo;
import com.luojia.soundscape.model.account.UserAccount;
import com.luojia.soundscape.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;

    @Autowired
    private UserAccountService userAccountService;

    /**
     * 根据订单编号查询充值记录
     *
     * @param orderNo
     * @return
     */
    @Override
    public RechargeInfo getRechargeInfo(String orderNo) {
        return rechargeInfoMapper.selectOne(
                new LambdaQueryWrapper<RechargeInfo>()
                        .eq(RechargeInfo::getOrderNo, orderNo)
        );
    }

    /**
     * 保存充值记录
     *
     * @param rechargeInfoVo
     * @return {“orderNo” :"充值订单编号"}
     */
    @Override
    public Map<String, String> submitRecharge(Long userId, RechargeInfoVo rechargeInfoVo) {
        //1.创建充值对象
        RechargeInfo rechargeInfo = new RechargeInfo();
        //2.属性赋值
        rechargeInfo.setUserId(userId);
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
        rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
        rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());
        //生成订单编号 "CZ"+日期+雪花算法
        String orderNo = "cz" + DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextIdStr();
        rechargeInfo.setOrderNo(orderNo);
        //3.保存充值对象
        rechargeInfoMapper.insert(rechargeInfo);
        //4.TODO 基于RabbitMQ 延迟消息 自动关单
        return Map.of("orderNo", orderNo);
    }

    @Override
    public void rechargePaySuccess(String orderNo) {
        int updated = rechargeInfoMapper.update(
                null,
                new LambdaUpdateWrapper<RechargeInfo>()
                        .eq(RechargeInfo::getOrderNo, orderNo)
                        .eq(RechargeInfo::getRechargeStatus, SystemConstant.ORDER_STATUS_UNPAID)
                        .set(RechargeInfo::getRechargeStatus, SystemConstant.ORDER_STATUS_PAID)
        );
        if (updated == 0) {
            return;
        }

        RechargeInfo rechargeInfo = getRechargeInfo(orderNo);
        boolean accountUpdated = userAccountService.update(
                new LambdaUpdateWrapper<UserAccount>()
                        .eq(UserAccount::getUserId, rechargeInfo.getUserId())
                        .setSql("total_amount = total_amount + " + rechargeInfo.getRechargeAmount())
                        .setSql("available_amount = available_amount + " + rechargeInfo.getRechargeAmount())
                        .setSql("total_income_amount = total_income_amount + " + rechargeInfo.getRechargeAmount())
        );
        if (accountUpdated) {
            userAccountService.saveUserAccountDetail(
                    rechargeInfo.getUserId(),
                    SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT,
                    rechargeInfo.getRechargeAmount(),
                    "充值",
                    orderNo
            );
        }
    }
}
