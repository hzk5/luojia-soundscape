package com.luojia.soundscape.user.pattern.impl;

import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.model.user.UserPaidAlbum;
import com.luojia.soundscape.user.mapper.UserPaidAlbumMapper;
import com.luojia.soundscape.user.pattern.DeliveryStrategy;
import com.luojia.soundscape.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 虚拟物品专辑发货策略实现类
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_ALBUM)
public class AlbumDelivery implements DeliveryStrategy {

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    /**
     * VIP会员虚拟物品发货逻辑
     * @param userPaidRecordVo
     */
    @Override
    public void delivery(UserPaidRecordVo userPaidRecordVo) {
        log.info("调用 专辑 虚拟物品发货逻辑");

        //1.1 判断该笔订单是否已处理
        Long count = userPaidAlbumMapper.selectCount(
                new LambdaQueryWrapper<UserPaidAlbum>()
                        .eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo())
        );
        if (count == 0) {
            //1.2 构建专辑购买记录
            UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
            userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
            userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
            userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
            //3.3 保存专辑购买记录
            userPaidAlbumMapper.insert(userPaidAlbum);
        }
    }
}
