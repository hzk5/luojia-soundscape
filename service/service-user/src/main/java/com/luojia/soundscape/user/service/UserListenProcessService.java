package com.luojia.soundscape.user.service;

import com.luojia.soundscape.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    /**
     * 查询指定用户某个声音播放进度
     *
     * @param trackId 声音ID
     * @return 秒
     */
    BigDecimal getTrackBreakSecond(Long userId, Long trackId);

    /**
     * 更新用户某个声音播放进度
     * @param userId 用户ID
     * @param userListenProcessVo 播放进度VO
     * @return
     */
    void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo);

    /**
     * 获取当前用户最近播放声音
     * @return {albumId:1,trackId:12}
     */
    Map<String, Long> getLatelyTrack(Long userId);
}
