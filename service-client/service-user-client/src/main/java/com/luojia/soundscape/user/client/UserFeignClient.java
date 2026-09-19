package com.luojia.soundscape.user.client;

import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.user.VipServiceConfig;
import com.luojia.soundscape.user.client.impl.UserDegradeFeignClient;
import com.luojia.soundscape.vo.user.UserInfoVo;
import com.luojia.soundscape.vo.user.UserPaidRecordVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户模块远程调用API接口
 * </p>
 *
 * @author Luojia Soundscape Contributors
 */
@FeignClient(value = "luojia-soundscape-user",path = "api/user", fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {

    /**
     * 根据用户ID查询用户信息
     * @param userId
     * @return
     */
    @GetMapping("/userInfo/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId);

    /**
     * 检查每个提交声音购买状态，如果已购买将购买状态设置为1，反之设置为0
     * @param userId 用户ID
     * @param albumId 专辑ID
     * @param needCheckPayStatusTrackIdList 待检查购买状态声音ID列表
     * @return 每个声音购买状态 {声音ID:购买状态}
     */
    @PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(
            @PathVariable Long userId,
            @PathVariable Long albumId,
            @RequestBody List<Long> needCheckPayStatusTrackIdList
    );

    /**
     * 根据套餐ID查询VIP详情
     * @param id
     * @return
     */
    @GetMapping("/vipServiceConfig/getVipServiceConfig/{id}")
    public Result<VipServiceConfig> getVipServiceConfig(@PathVariable Long id);


    /**
     * 检查用户是否已购买专辑
     * 提供OpenFeign接口如何隐式传递认证信息（token或userid） 解决：通过feign拦截器
     *
     * @param albumId
     * @return true:已购买 false:未购买
     */
    @GetMapping("/userInfo/isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId);

    /**
     * 查询用户已购买的声音ID列表
     * @param albumId 专辑ID
     * @return 已购声音ID列表
     */
    @GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
    public Result<List<Long>> findUserPaidTrackList(@PathVariable Long albumId);

    /**
     * 用户付款（余额、微信）成功后，发放权益（VIP、专辑、声音）
     * @param userPaidRecordVo
     * @return
     */
    @PostMapping("/userInfo/savePaidRecord")
    public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo);

    /** 更新已到期的 VIP 用户状态。 */
    @GetMapping("/userInfo/updateVipExpireStatus")
    Result<Integer> updateVipExpireStatus();
}
