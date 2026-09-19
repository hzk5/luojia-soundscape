package com.luojia.soundscape.account.mapper;

import com.luojia.soundscape.model.account.UserAccount;
import com.luojia.soundscape.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 使用悲观锁防止账户余额超扣
     * @param accountDeductVo
     * @return
     */
    UserAccount checkAmount(@Param("vo") AccountDeductVo accountDeductVo);
}
