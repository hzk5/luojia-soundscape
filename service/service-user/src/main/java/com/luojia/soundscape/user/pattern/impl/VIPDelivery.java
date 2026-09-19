package com.luojia.soundscape.user.pattern.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.common.rabbit.service.CacheInvalidationService;
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
 * 虚拟物品VIP会员发货策略实现类
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_VIP)  //默认BeanID=vIPDelivery
public class VIPDelivery implements DeliveryStrategy {

    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private CacheInvalidationService cacheInvalidationService;

    /**
     * VIP会员虚拟物品发货逻辑
     * @param userPaidRecordVo
     */
    @Override
    public void delivery(UserPaidRecordVo userPaidRecordVo) {
        log.info("调用VIP会员虚拟物品发货逻辑");
        //3.处理付款项目类型为VIP会员，权益发放
        Date now = new Date();
        //3.1 判断该笔订单是否已处理
        Long count = userVipServiceMapper.selectCount(
                new LambdaQueryWrapper<UserVipService>()
                        .eq(UserVipService::getOrderNo, userPaidRecordVo.getOrderNo())
        );
        if (count == 0) {
            //3.2 查询用户得到目前身份
            Boolean isVIP = false;
            UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
            if (userInfo.getIsVip().intValue() == 1 && userInfo.getVipExpireTime().after(now)) {
                isVIP = true;
            }
            //3.3 创建会员购买记录 封装：生效时间、结束时间。保存购买记录
            UserVipService userVipService = new UserVipService();
            userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
            userVipService.setUserId(userPaidRecordVo.getUserId());
            //3.3.0 获取选购套餐信息
            VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(userPaidRecordVo.getItemIdList().get(0));
            Integer serviceMonth = vipServiceConfig.getServiceMonth();
            //3.3.1 计算本次会员生效时间
            if (!isVIP) {
                //当前是普通用户 起始时间=当前时间 过期时间=当前时间+会员时长
                userVipService.setStartTime(now);
                //3.3.2 计算本次会员到期时间
                DateTime endTime = DateUtil.offsetMonth(now, serviceMonth);
                userVipService.setExpireTime(endTime);
            }else{
                //当前已是VIP用户 起始时间=当前会员到期时间+1天
                DateTime startTime = DateUtil.offsetDay(userInfo.getVipExpireTime(), 1);
                userVipService.setStartTime(startTime);
                DateTime endTime = DateUtil.offsetMonth(startTime, serviceMonth);
                userVipService.setExpireTime(endTime);
            }
            /// 3.4 保存会员购买记录
            userVipServiceMapper.insert(userVipService);
            //3.4 更新用户会员标识、过期时间
            userInfo.setIsVip(1);
            userInfo.setVipExpireTime(userVipService.getExpireTime());
            userInfoMapper.updateById(userInfo);
            cacheInvalidationService.evictAfterCommit("user:userinfovo:" + userInfo.getId());
        }
    }
}
