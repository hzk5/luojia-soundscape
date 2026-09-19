package com.luojia.soundscape.user.pattern.impl;

import com.luojia.soundscape.album.AlbumFeignClient;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.model.album.TrackInfo;
import com.luojia.soundscape.model.user.UserPaidTrack;
import com.luojia.soundscape.user.client.UserFeignClient;
import com.luojia.soundscape.user.mapper.UserPaidTrackMapper;
import com.luojia.soundscape.user.pattern.DeliveryStrategy;
import com.luojia.soundscape.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 虚拟物品声音发货策略实现类
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_TRACK)
public class TrackDelivery implements DeliveryStrategy {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    /**
     * VIP会员虚拟物品发货逻辑
     * @param userPaidRecordVo
     */
    @Override
    public void delivery(UserPaidRecordVo userPaidRecordVo) {
        log.info("调用声音虚拟物品发货逻辑");
        //2.处理付款项目类型为声音，权益发放
        //2.1 判断该笔订单是否已处理
        Long count = userPaidTrackMapper.selectCount(
                new LambdaQueryWrapper<UserPaidTrack>()
                        .eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo())
        );
        //2.2 构建声音多条购买记录
        if (count == 0) {
            //根据声音ID查询声音对象获取所属专辑ID
            TrackInfo trackInfo = albumFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0)).getData();
            Long albumId = trackInfo.getAlbumId();
            for (Long trackId : userPaidRecordVo.getItemIdList()) {
                UserPaidTrack userPaidTrack = new UserPaidTrack();
                userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
                userPaidTrack.setUserId(userPaidRecordVo.getUserId());
                userPaidTrack.setAlbumId(albumId);
                userPaidTrack.setTrackId(trackId);
                //2.3 保存声音购买记录
                userPaidTrackMapper.insert(userPaidTrack);
            }
        }
    }
}
