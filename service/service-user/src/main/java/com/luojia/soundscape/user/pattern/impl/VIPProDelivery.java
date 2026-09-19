package com.luojia.soundscape.user.pattern.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.model.user.UserInfo;
import com.luojia.soundscape.model.user.UserVipService;
import com.luojia.soundscape.model.user.VipServiceConfig;
import com.luojia.soundscape.user.mapper.UserInfoMapper;
import com.luojia.soundscape.user.mapper.UserVipServiceMapper;
import com.luojia.soundscape.user.mapper.VipServiceConfigMapper;
import com.luojia.soundscape.user.pattern.DeliveryStrategy;
import com.luojia.soundscape.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 虚拟物品超级VIP会员发货策略实现类
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_VIP_PRO)  //默认BeanID=vIPDelivery
public class VIPProDelivery implements DeliveryStrategy {


    /**
     * 超级VIP会员虚拟物品发货逻辑
     * @param userPaidRecordVo
     */
    @Override
    public void delivery(UserPaidRecordVo userPaidRecordVo) {
        log.info("调用 超级VIP会员 虚拟物品发货逻辑");
    }
}
