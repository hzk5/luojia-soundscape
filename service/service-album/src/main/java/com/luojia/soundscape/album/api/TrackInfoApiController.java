package com.luojia.soundscape.album.api;

import com.luojia.soundscape.album.service.TrackInfoService;
import com.luojia.soundscape.album.service.VodService;
import com.luojia.soundscape.common.login.SoundscapeLogin;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.common.util.AuthContextHolder;
import com.luojia.soundscape.model.album.TrackInfo;
import com.luojia.soundscape.query.album.TrackInfoQuery;
import com.luojia.soundscape.query.album.BatchIdQuery;
import com.luojia.soundscape.vo.album.AlbumTrackListVo;
import com.luojia.soundscape.vo.album.TrackInfoVo;
import com.luojia.soundscape.vo.album.TrackBriefVo;
import com.luojia.soundscape.vo.album.TrackListVo;
import com.luojia.soundscape.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

    @Autowired
    private TrackInfoService trackInfoService;

    @Autowired
    private VodService vodService;


    /**
     * 文件上传，将音视频文件上传到点播平台
     *
     * @param file 文件
     * @return {mediaFileId:"文件唯一标识",mediaUrl:"播放地址"}
     */
    @Operation(summary = "将音视频文件上传到点播平台")
    @PostMapping("/trackInfo/uploadTrack")
    public Result<Map<String, String>> uploadTrack(@RequestParam("file") MultipartFile file) {
        Map<String, String> map = vodService.uploadTrack(file);
        return Result.ok(map);
    }

    /**
     * 该接口必须登录后才能访问
     * 保存声音信息
     *
     * @param trackInfoVo
     * @return
     */
    @SoundscapeLogin
    @PostMapping("/trackInfo/saveTrackInfo")
    @Operation(summary = "保存声音信息")
    public Result saveTrackInfo(@RequestBody @Validated TrackInfoVo trackInfoVo) {
        //1. 获取当用户ID
        Long userId = AuthContextHolder.getUserId();
        //2. 调用业务逻辑
        trackInfoService.saveTrackInfo(userId, trackInfoVo);
        //3. 返回结果
        return Result.ok();
    }

    /**
     * 该接口必须登录后才能访问
     * 分页查询当前用户声音列表（包含统计信息）
     *
     * @param page           页码
     * @param limit          页大小
     * @param trackInfoQuery 查询条件
     * @return MP分页对象
     */
    @SoundscapeLogin
    @Operation(summary = "分页查询当前用户声音列表（包含统计信息）")
    @PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
    public Result<IPage<TrackListVo>> findUserTrackPage(
            @PathVariable Long page,
            @PathVariable Long limit,
            @RequestBody TrackInfoQuery trackInfoQuery
    ) {
        //1. 获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        trackInfoQuery.setUserId(userId);
        //2. 创建分页对象 封装页码、页大小
        IPage<TrackListVo> pageInfo = new Page<>(page, limit);
        //3. 调用业务逻辑
        pageInfo = trackInfoService.findUserTrackPage(pageInfo, trackInfoQuery);
        //4. 返回结果
        return Result.ok(pageInfo);
    }


    /**
     * 根据声音ID查询声音信息
     *
     * @param id
     * @return
     */
    @Operation(summary = "根据声音ID查询声音信息")
    @GetMapping("/trackInfo/getTrackInfo/{id}")
    public Result<TrackInfo> getTrackInfo(@PathVariable Long id) {
        TrackInfo trackInfo = trackInfoService.getById(id);
        return Result.ok(trackInfo);
    }

    @Operation(summary = "批量查询声音列表摘要")
    @PostMapping("/trackInfo/batch")
    public Result<List<TrackBriefVo>> getTrackInfoBatch(@RequestBody @Validated BatchIdQuery query) {
        return Result.ok(trackInfoService.getTrackInfoBatch(query.getIds()));
    }


    @Operation(summary = "更新声音信息")
    @PutMapping("/trackInfo/updateTrackInfo/{id}")
    public Result updateTrackInfo(@PathVariable Long id, @RequestBody @Validated TrackInfoVo trackInfoVo) {
        trackInfoService.updateTrackInfo(id, trackInfoVo);
        return Result.ok();
    }


    /**
     * 删除声音信息（包括音频文件）
     *
     * @param id 声音ID
     * @return
     */
    @Operation(summary = "删除声音信息（包括音频文件）")
    @DeleteMapping("/trackInfo/removeTrackInfo/{id}")
    public Result removeTrackInfo(@PathVariable Long id) {
        trackInfoService.removeTrackInfo(id);
        return Result.ok();
    }

    /**
     * 根据专辑ID分页查询声音列表包含统计信息（动态渲染付费标识）
     *
     * @param albumId 专辑ID
     * @param page    页码
     * @param limit   页大小
     * @return 分页对象
     */
    @SoundscapeLogin(required = false)
    @Operation(summary = "根据专辑ID分页查询声音列表包含统计信息（动态渲染付费标识）")
    @GetMapping("/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}")
    public Result<IPage<AlbumTrackListVo>> findAlbumTrackPage(
            @PathVariable Long albumId,
            @PathVariable Long page,
            @PathVariable Long limit
    ) {
        //1.获取当前用户ID（可能为空）
        Long userId = AuthContextHolder.getUserId();
        //2.创建分页对象 封装页码、页大小
        IPage<AlbumTrackListVo> pageInfo = new Page<>(page, limit);
        //3.调用业务逻辑
        pageInfo = trackInfoService.findAlbumTrackPage(pageInfo, albumId, userId);
        //4.返回分页结果
        return Result.ok(pageInfo);
    }

    /**
     * 查询声音统计信息
     *
     * @param trackId
     * @return
     */
    @Operation(summary = "查询声音统计信息")
    @GetMapping("/trackInfo/getTrackStatVo/{trackId}")
    public Result<TrackStatVo> getTrackStatVo(@PathVariable Long trackId) {
        TrackStatVo trackStatVo = trackInfoService.getTrackStatVo(trackId);
        return Result.ok(trackStatVo);
    }


    /**
     * 基于用户未购买声音数量动态构建分集购买列表
     *
     * @param trackId
     * @return
     */
    @SoundscapeLogin
    @Operation(summary = "基于用户未购买声音数量动态构建分集购买列表")
    @GetMapping("/trackInfo/findUserTrackPaidList/{trackId}")
    public Result<List<Map<String, Object>>> findUserTrackPaidList(@PathVariable Long trackId) {
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务逻辑
        List<Map<String, Object>> list = trackInfoService.findUserTrackPaidList(trackId, userId);
        //3.返回结果
        return Result.ok(list);
    }

    /**
     * 以提交声音ID作为标准，查询未购买声音列表
     * @param trackId 提交声音ID
     * @param trackCount 声音数量
     * @return 声音列表
     */
    @SoundscapeLogin
    @Operation(summary = "以提交声音ID作为标准，查询未购买声音列表")
    @GetMapping("/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
    public Result<List<TrackInfo>>findPaidTrackInfoList(@PathVariable Long trackId,@PathVariable Integer trackCount){
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        List<TrackInfo> list = trackInfoService.findPaidTrackInfoList(trackId, trackCount, userId);
        return Result.ok(list);
    }
}
