package com.luojia.soundscape.user.api;

import com.luojia.soundscape.common.login.SoundscapeLogin;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.common.util.AuthContextHolder;
import com.luojia.soundscape.user.service.UserListenProcessService;
import com.luojia.soundscape.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserListenProcessApiController {

    @Autowired
    private UserListenProcessService userListenProcessService;


    /**
     * 查询当前用户某个声音播放进度
     *
     * @param trackId 声音ID
     * @return 秒
     */
    @SoundscapeLogin(required = false)
    @Operation(summary = "查询当前用户某个声音播放进度")
    @GetMapping("/userListenProcess/getTrackBreakSecond/{trackId}")
    public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId) {
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.如果用户ID有值，则查询用户播放进度
        if (userId != null) {
            BigDecimal breakSecond = userListenProcessService.getTrackBreakSecond(userId, trackId);
            return Result.ok(breakSecond);
        }
        return Result.ok(BigDecimal.ZERO);
    }


    /**
     * 更新用户某个声音播放进度
     *
     * @param userListenProcessVo
     * @return
     */
    @SoundscapeLogin(required = false)
    @PostMapping("/userListenProcess/updateListenProcess")
    public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo) {
        //1.获取用户ID
        Long userId = AuthContextHolder.getUserId();
        if (userId != null) {
            //4.调用service方法
            userListenProcessService.updateListenProcess(userId, userListenProcessVo);
        }
        return Result.ok();
    }

    /**
     * 获取当前用户最近播放声音
     * @return {albumId:1,trackId:12}
     */
    @SoundscapeLogin
    @GetMapping("/userListenProcess/getLatelyTrack")
    public Result<Map<String, Long>> getLatelyTrack(){
        Long userId = AuthContextHolder.getUserId();
        Map<String, Long> map = userListenProcessService.getLatelyTrack(userId);
        return Result.ok(map);
    }
}

