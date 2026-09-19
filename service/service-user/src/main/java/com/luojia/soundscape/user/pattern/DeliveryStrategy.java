package com.luojia.soundscape.user.pattern;

import com.luojia.soundscape.vo.user.UserPaidRecordVo;

/**
 * 权益方法/虚拟物品发货策略接口
 * 提供抽象方法：发货方法
 */
public interface DeliveryStrategy {

    /**
     * 发放权益抽象方法
     * @param userPaidRecordVo
     */
    void delivery(UserPaidRecordVo userPaidRecordVo);

}
