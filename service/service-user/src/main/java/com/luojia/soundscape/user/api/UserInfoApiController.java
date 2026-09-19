package com.luojia.soundscape.user.api;

import com.luojia.soundscape.common.login.SoundscapeLogin;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.common.util.AuthContextHolder;
import com.luojia.soundscape.user.service.UserInfoService;
import com.luojia.soundscape.vo.user.UserInfoVo;
import com.luojia.soundscape.vo.user.UserCollectVo;
import com.luojia.soundscape.vo.user.UserPaidRecordVo;
import com.luojia.soundscape.vo.user.UserSubscribeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Date;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId
     * @return
     */
    @Operation(summary = "根据用户ID查询用户信息")
    @GetMapping("/userInfo/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId) {
        UserInfoVo userInfoVo = userInfoService.getUserInfoVo(userId);
        return Result.ok(userInfoVo);
    }

    /**
     * 检查每个提交声音购买状态，如果已购买将购买状态设置为1，反之设置为0
     *
     * @param userId                        用户ID
     * @param albumId                       专辑ID
     * @param needCheckPayStatusTrackIdList 待检查购买状态声音ID列表
     * @return 每个声音购买状态 {声音ID:购买状态}
     */
    @Operation(summary = "检查每个提交声音购买状态，如果已购买将购买状态设置为1，反之设置为0")
    @PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(
            @PathVariable Long userId,
            @PathVariable Long albumId,
            @RequestBody List<Long> needCheckPayStatusTrackIdList
    ) {
        Map<Long, Integer> map = userInfoService.userIsPaidTrack(userId, albumId, needCheckPayStatusTrackIdList);
        return Result.ok(map);
    }

    /**
     * 检查用户是否已购买专辑
     * TODO 提供OpenFeign接口如何隐式传递认证信息（token或userid）
     *
     * @param albumId
     * @return true:已购买 false:未购买
     */
    @SoundscapeLogin //要求调用方必须携带合法Token调用
    @Operation(summary = "检查用户是否已购买专辑")
    @GetMapping("/userInfo/isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId) {
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用服务层方法
        Boolean isPaid = userInfoService.isPaidAlbum(userId, albumId);
        return Result.ok(isPaid);
    }

    /**
     * 查询用户已购买的声音ID列表
     * @param albumId 专辑ID
     * @return 已购声音ID列表
     */
    @SoundscapeLogin
    @Operation(summary = "查询用户已购买的声音ID列表")
    @GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
    public Result<List<Long>> findUserPaidTrackList(@PathVariable Long albumId) {
        Long userId = AuthContextHolder.getUserId();
        List<Long> list = userInfoService.findUserPaidTrackList(userId, albumId);
        return Result.ok(list);
    }


    /**
     * 用户付款（余额、微信）成功后，发放权益（VIP、专辑、声音）
     * @param userPaidRecordVo
     * @return
     */
    @Operation(summary = "用户付款（余额、微信）成功后，发放权益（VIP、专辑、声音）")
    @PostMapping("/userInfo/savePaidRecord")
    public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo){
        userInfoService.savePaidRecord(userPaidRecordVo);
        return Result.ok();
    }

    @SoundscapeLogin
    @Operation(summary = "订阅或取消订阅专辑")
    @GetMapping("/userInfo/subscribe/{albumId}")
    public Result<Boolean> subscribe(@PathVariable Long albumId) {
        return Result.ok(userInfoService.subscribe(AuthContextHolder.getUserId(), albumId));
    }

    @SoundscapeLogin
    @Operation(summary = "查询当前用户是否订阅专辑")
    @GetMapping("/userInfo/isSubscribe/{albumId}")
    public Result<Boolean> isSubscribe(@PathVariable Long albumId) {
        return Result.ok(userInfoService.isSubscribe(AuthContextHolder.getUserId(), albumId));
    }

    @SoundscapeLogin
    @Operation(summary = "分页查询当前用户订阅的专辑")
    @GetMapping("/userInfo/findUserSubscribePage/{page}/{limit}")
    public Result<IPage<UserSubscribeVo>> findUserSubscribePage(@PathVariable long page, @PathVariable long limit) {
        return Result.ok(userInfoService.findUserSubscribePage(AuthContextHolder.getUserId(), page, limit));
    }

    @SoundscapeLogin
    @Operation(summary = "收藏或取消收藏声音")
    @GetMapping("/userInfo/collect/{trackId}")
    public Result<Boolean> collect(@PathVariable Long trackId) {
        return Result.ok(userInfoService.collect(AuthContextHolder.getUserId(), trackId));
    }

    @SoundscapeLogin
    @Operation(summary = "查询当前用户是否收藏声音")
    @GetMapping("/userInfo/isCollect/{trackId}")
    public Result<Boolean> isCollect(@PathVariable Long trackId) {
        return Result.ok(userInfoService.isCollect(AuthContextHolder.getUserId(), trackId));
    }

    @SoundscapeLogin
    @Operation(summary = "分页查询当前用户收藏的声音")
    @GetMapping("/userInfo/findUserCollectPage/{page}/{limit}")
    public Result<IPage<UserCollectVo>> findUserCollectPage(@PathVariable long page, @PathVariable long limit) {
        return Result.ok(userInfoService.findUserCollectPage(AuthContextHolder.getUserId(), page, limit));
    }

    @Operation(summary = "将已到期的 VIP 更新为失效状态")
    @GetMapping("/userInfo/updateVipExpireStatus")
    public Result<Integer> updateVipExpireStatus() {
        return Result.ok(userInfoService.updateVipExpireStatus(new Date()));
    }
}
