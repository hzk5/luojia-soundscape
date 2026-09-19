package com.luojia.soundscape.album.impl;


import com.luojia.soundscape.album.AlbumFeignClient;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.album.*;
import com.luojia.soundscape.query.album.BatchIdQuery;
import com.luojia.soundscape.vo.album.AlbumBriefVo;
import com.luojia.soundscape.vo.album.AlbumDetailVo;
import com.luojia.soundscape.vo.album.AlbumStatVo;
import com.luojia.soundscape.vo.album.TrackBriefVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AlbumDegradeFeignClient implements AlbumFeignClient {


    @Override
    public Result<List<BaseCategory1>> findAllCategory1() {
        log.error("[专辑服务]提供远程调用接口findAllCategory1执行服务降级");
        return Result.<List<BaseCategory1>>fail(List.of()).message("专辑服务暂时不可用");
    }

    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
        log.error("[专辑服务]提供远程调用接口getAlbumInfo执行服务降级");
        return Result.<AlbumInfo>fail().message("专辑服务暂时不可用");
    }

    @Override
    public Result<List<AlbumBriefVo>> getAlbumInfoBatch(BatchIdQuery query) {
        log.error("[专辑服务]批量查询专辑执行服务降级");
        return Result.<List<AlbumBriefVo>>fail(List.of()).message("专辑服务暂时不可用");
    }

    @Override
    public Result<AlbumDetailVo> getAlbumDetail(Long albumId) {
        log.error("[专辑服务]聚合查询专辑详情执行服务降级");
        return Result.<AlbumDetailVo>fail().message("专辑服务暂时不可用");
    }

    @Override
    public Result<BaseCategoryView> getCategoryView(Long category3Id) {
        log.error("[专辑服务]提供远程调用接口getCategoryView执行服务降级");
        return Result.<BaseCategoryView>fail().message("专辑服务暂时不可用");
    }

    @Override
    public Result<List<BaseCategory3>> findTop7BaseCategory3(Long category1Id) {
        log.error("[专辑服务]提供远程调用接口findTop7BaseCategory3执行服务降级");
        return Result.<List<BaseCategory3>>fail(List.of()).message("专辑服务暂时不可用");
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        log.error("[专辑服务]提供远程调用接口getAlbumStatVo执行服务降级");
        return Result.<AlbumStatVo>fail().message("专辑服务暂时不可用");
    }

    @Override
    public Result<List<TrackInfo>> findPaidTrackInfoList(Long trackId, Integer trackCount) {
        log.error("[专辑服务]提供远程调用接口findPaidTrackInfoList执行服务降级");
        return Result.<List<TrackInfo>>fail(List.of()).message("专辑服务暂时不可用");
    }

    @Override
    public Result<TrackInfo> getTrackInfo(Long id) {
        return Result.<TrackInfo>fail().message("专辑服务暂时不可用");
    }

    @Override
    public Result<List<TrackBriefVo>> getTrackInfoBatch(BatchIdQuery query) {
        log.error("[专辑服务]批量查询声音执行服务降级");
        return Result.<List<TrackBriefVo>>fail(List.of()).message("专辑服务暂时不可用");
    }
}
