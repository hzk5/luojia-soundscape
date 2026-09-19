package com.luojia.soundscape.account.service;

import com.luojia.soundscape.model.account.UserAccount;
import com.luojia.soundscape.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Map;

public interface UserAccountService extends IService<UserAccount> {


    /**
     * 初始化账户记录
     * @param map 业务数据
     */
    void initUserAccount(Map<String, Object> map);

    /**
     * 查询指定用户账户可用余额
     * @param userId
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);

    /**
     * 检查且扣减账户金额
     * @param accountDeductVo
     * @return
     */
    void checkAndDeduct(AccountDeductVo accountDeductVo);

    /** 记录一笔账户变动。 */
    void saveUserAccountDetail(Long userId, String tradeType, BigDecimal amount, String title, String orderNo);
}
