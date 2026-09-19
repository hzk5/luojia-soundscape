package com.luojia.soundscape.account.service.impl;

import cn.hutool.core.lang.Assert;
import com.luojia.soundscape.account.mapper.UserAccountDetailMapper;
import com.luojia.soundscape.account.mapper.UserAccountMapper;
import com.luojia.soundscape.account.service.UserAccountService;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.common.execption.SoundscapeException;
import com.luojia.soundscape.model.account.UserAccount;
import com.luojia.soundscape.model.account.UserAccountDetail;
import com.luojia.soundscape.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

    /**
     * 初始化账户记录
     *
     * @param map 业务数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initUserAccount(Map<String, Object> map) {
        Long userId = (Long) map.get("userId");
        BigDecimal amount = (BigDecimal) map.get("amount");
        String title = (String) map.get("title");
        String orderNo = (String) map.get("orderNo");
        //1.新增账户记录
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        userAccount.setTotalAmount(amount);
        userAccount.setLockAmount(BigDecimal.valueOf(0.00));
        userAccount.setAvailableAmount(amount);
        userAccount.setTotalIncomeAmount(amount);
        userAccount.setTotalPayAmount(BigDecimal.valueOf(0.00));
        int insert = userAccountMapper.insert(userAccount);
        //2.新增账户变动日志
        if (insert > 0) {
            this.saveUserAccountDetail(userId, SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, amount, title, orderNo);
        }

    }

    /**
     * 保存账户变动日志
     * @param userId
     * @param tradeType
     * @param amount
     * @param title
     * @param orderNo
     */
    @Override
    public void saveUserAccountDetail(Long userId, String tradeType, BigDecimal amount, String title, String orderNo) {
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        //交易类型：1201-充值 1202-锁定 1203-解锁 1204-消费
        userAccountDetail.setTradeType(tradeType);
        userAccountDetail.setAmount(amount);
        userAccountDetail.setTitle(title);
        userAccountDetail.setOrderNo(orderNo);
        userAccountDetailMapper.insert(userAccountDetail);
    }

    /**
     * 查询指定用户账户可用余额
     *
     * @param userId
     * @return
     */
    @Override
    public BigDecimal getAvailableAmount(Long userId) {
        UserAccount userAccount = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getUserId, userId)
        );
        Assert.notNull(userAccount, "用户账户不存在！");
        return userAccount.getAvailableAmount();
    }

    /**
     * 检查且扣减账户金额
     *
     * @param accountDeductVo
     * @return
     */
    @Override
    public void checkAndDeduct(AccountDeductVo accountDeductVo) {
        //1.查询账户记录 判断是否满足扣减条件  如果有分布式事务框架没必要自己写锁逻辑
        UserAccount userAccount = userAccountMapper.checkAmount(accountDeductVo);
        if (userAccount == null) {
            throw new SoundscapeException(500, "账户余额不足！");
        }
        //2.扣减账户金额
        int update = userAccountMapper.update(
                null,
                new LambdaUpdateWrapper<UserAccount>()
                        .eq(UserAccount::getUserId, accountDeductVo.getUserId())
                        .setSql("available_amount = available_amount - " + accountDeductVo.getAmount())
                        .setSql("total_amount = total_amount - " + accountDeductVo.getAmount())
                        .setSql("total_pay_amount = total_pay_amount + " + accountDeductVo.getAmount())
        );
        if (update > 0) {
            //3.记录账户变动日志
            this.saveUserAccountDetail(accountDeductVo.getUserId(), SystemConstant.ACCOUNT_TRADE_TYPE_MINUS, accountDeductVo.getAmount(), accountDeductVo.getContent(), accountDeductVo.getOrderNo());
        }
    }
}
