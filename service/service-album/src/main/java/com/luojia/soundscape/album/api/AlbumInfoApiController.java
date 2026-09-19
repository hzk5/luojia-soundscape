package com.luojia.soundscape.album.api;

import com.luojia.soundscape.album.service.AlbumInfoService;
import com.luojia.soundscape.album.service.AlbumDetailService;
import com.luojia.soundscape.common.login.SoundscapeLogin;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.common.util.AuthContextHolder;
import com.luojia.soundscape.model.album.AlbumInfo;
import com.luojia.soundscape.query.album.AlbumInfoQuery;
import com.luojia.soundscape.query.album.BatchIdQuery;
import com.luojia.soundscape.vo.album.AlbumBriefVo;
import com.luojia.soundscape.vo.album.AlbumInfoVo;
import com.luojia.soundscape.vo.album.AlbumDetailVo;
import com.luojia.soundscape.vo.album.AlbumListVo;
import com.luojia.soundscape.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;

    @Autowired
    private AlbumDetailService albumDetailService;


    /**
     * 该接口必须登录才能访问
     * 保存专辑信息
     *
     * @param albumInfoVo
     * @return
     */
    @Operation(summary = "保存专辑信息")
    @PostMapping("/albumInfo/saveAlbumInfo")
    @SoundscapeLogin
    public Result saveAlbumInfo(@RequestBody @Validated AlbumInfoVo albumInfoVo) {
        //1.获取当前用户ID 目前获取到是硬编码为1的用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务逻辑
        albumInfoService.saveAlbumInfo(albumInfoVo, userId);
        //3.返回结果
        return Result.ok();
    }

    /***
     *  该接口必须登录才能访问
     * 查看当前用户专辑分页列表（包含统计信息）
     * @param page 页码
     * @param limit 页大小
     * @param query 查询条件
     * @return MP分页对象
     */
    @Operation(summary = "查看当前用户专辑分页列表（包含统计信息）")
    @PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
    @SoundscapeLogin(required = true) // 登录拦截，如果未登录不允许访问，反之登录可以执行调用
    public Result<IPage<AlbumListVo>> findUserAlbumPage(
            @PathVariable Long page,
            @PathVariable Long limit,
            @RequestBody AlbumInfoQuery query
    ){
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.创建分页对象，封装页码、页大小
        IPage<AlbumListVo> pageInfo = new Page<>(page, limit);
        //3.调用业务逻辑，最终执行持久层查询 封装分页集合、总记录数、总页数
        query.setUserId(userId);
        pageInfo = albumInfoService.findUserAlbumPage(pageInfo, query);
        //4.返回分页结果
        return Result.ok(pageInfo);
    }


    /**
     * 删除专辑
     * @param id
     * @return
     */
    @Operation(summary = "删除专辑")
    @DeleteMapping("/albumInfo/removeAlbumInfo/{id}")
    public Result removeAlbumInfo(@PathVariable Long id){
        albumInfoService.removeAlbumInfo(id);
        return Result.ok();
    }


    /**
     * 根据专辑ID查询专辑信息（包含标签列表）
     * @param id 专辑ID
     * @return
     */
    @Operation(summary = "根据专辑ID查询专辑信息")
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id){
        AlbumInfo albumInfo = albumInfoService.getAlbumInfoFromDB(id);
        return Result.ok(albumInfo);
    }

    @Operation(summary = "批量查询专辑列表摘要")
    @PostMapping("/albumInfo/batch")
    public Result<List<AlbumBriefVo>> getAlbumInfoBatch(@RequestBody @Validated BatchIdQuery query) {
        return Result.ok(albumInfoService.getAlbumInfoBatch(query.getIds()));
    }

    @Operation(summary = "聚合查询专辑详情")
    @GetMapping("/albumInfo/getAlbumDetail/{albumId}")
    public Result<AlbumDetailVo> getAlbumDetail(@PathVariable Long albumId) {
        return Result.ok(albumDetailService.getAlbumDetail(albumId));
    }

    /**
     * 更新专辑信息
     * @param id
     * @param albumInfoVo
     * @return
     */
    @Operation(summary = "更新专辑信息")
    @PutMapping("/albumInfo/updateAlbumInfo/{id}")
    public Result updateAlbumInfo(@PathVariable Long id, @RequestBody @Validated AlbumInfoVo albumInfoVo){
        albumInfoService.updateAlbumInfo(id, albumInfoVo);
        return Result.ok();
    }


    /**
     *  该接口必须登录才能访问
     * @return
     */
    @SoundscapeLogin
    @Operation(summary = "查询当前用户专辑列表")
    @GetMapping("/albumInfo/findUserAllAlbumList")
    public Result<List<AlbumInfo>> findUserAllAlbumList(){
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务逻辑
        List<AlbumInfo> list = albumInfoService.findUserAllAlbumList(userId);
        //3.返回结果
        return Result.ok(list);
    }

    /**
     * 根据专辑ID查询统计信息
     * @param albumId
     * @return
     */
    @Operation(summary = "根据专辑ID查询统计信息")
    @GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId){
        AlbumStatVo albumStatVo = albumInfoService.getAlbumStatVo(albumId);
        return Result.ok(albumStatVo);
    }

}
