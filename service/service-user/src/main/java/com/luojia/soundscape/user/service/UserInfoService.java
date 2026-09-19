package com.luojia.soundscape.user.service;

import com.luojia.soundscape.model.user.UserInfo;
import com.luojia.soundscape.vo.user.UserInfoVo;
import com.luojia.soundscape.vo.user.UserCollectVo;
import com.luojia.soundscape.vo.user.UserPaidRecordVo;
import com.luojia.soundscape.vo.user.UserSubscribeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;
import java.util.Date;

public interface UserInfoService extends IService<UserInfo> {

    /**
     * 微信一键登录
     * @param code 小程序端对接微信获取临时登录凭证code 5分钟只能使用一次
     * @return {token:"访问令牌"}
     */
    Map<String, String> wxLogin(String code);

    UserInfoVo getUserInfoVo(Long userId);


    void updateUser(Long userId, UserInfoVo userInfoVo);

    /**
     * 检查每个提交声音购买状态，如果已购买将购买状态设置为1，反之设置为0
     * @param userId 用户ID
     * @param albumId 专辑ID
     * @param needCheckPayStatusTrackIdList 待检查购买状态声音ID列表
     * @return 每个声音购买状态 {声音ID:购买状态}
     */
    Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStatusTrackIdList);

    /**
     * 检查用户是否已购买专辑
     * @param albumId
     * @return
     */
    Boolean isPaidAlbum(Long userId, Long albumId);

    /**
     * 查询用户已购买的声音ID列表
     * @param albumId 专辑ID
     * @return 已购声音ID列表
     */
    List<Long> findUserPaidTrackList(Long userId, Long albumId);

    /**
     * 用户付款（余额、微信）成功后，发放权益（VIP、专辑、声音）
     * @param userPaidRecordVo
     * @return
     */
    void savePaidRecord(UserPaidRecordVo userPaidRecordVo);

    Boolean subscribe(Long userId, Long albumId);

    Boolean isSubscribe(Long userId, Long albumId);

    IPage<UserSubscribeVo> findUserSubscribePage(Long userId, long page, long limit);

    Boolean collect(Long userId, Long trackId);

    Boolean isCollect(Long userId, Long trackId);

    IPage<UserCollectVo> findUserCollectPage(Long userId, long page, long limit);

    /**
     * 将到期时间不晚于指定时间的 VIP 更新为失效。
     *
     * @return 本次更新的用户数
     */
    int updateVipExpireStatus(Date date);
}
