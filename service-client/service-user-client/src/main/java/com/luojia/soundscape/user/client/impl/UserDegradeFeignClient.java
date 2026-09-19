package com.luojia.soundscape.user.client.impl;


import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.user.VipServiceConfig;
import com.luojia.soundscape.user.client.UserFeignClient;
import com.luojia.soundscape.vo.user.UserInfoVo;
import com.luojia.soundscape.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class UserDegradeFeignClient implements UserFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfoVo(Long userId) {
        log.error("[用户服务]提供远程调用接口getUserInfoVo执行服务降级");
        return null;
    }

    @Override
    public Result<Map<Long, Integer>> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStatusTrackIdList) {
        log.error("[用户服务]提供远程调用接口userIsPaidTrack执行服务降级");
        return null;
    }

    @Override
    public Result<VipServiceConfig> getVipServiceConfig(Long id) {
        log.error("[用户服务]提供远程调用接口getVipServiceConfig执行服务降级");
        return null;
    }

    @Override
    public Result<Boolean> isPaidAlbum(Long albumId) {
        log.error("[用户服务]提供远程调用接口isPaidAlbum执行服务降级");
        return null;
    }

    @Override
    public Result<List<Long>> findUserPaidTrackList(Long albumId) {
        log.error("[用户服务]提供远程调用接口findUserPaidTrackList执行服务降级");
        return null;
    }

    @Override
    public Result savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        return null;
    }

    @Override
    public Result<Integer> updateVipExpireStatus() {
        log.error("用户服务不可用，无法更新 VIP 过期状态");
        return Result.<Integer>fail().message("用户服务不可用");
    }
}
