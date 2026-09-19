package com.luojia.soundscape.album.service;

import com.luojia.soundscape.model.album.TrackInfo;
import com.luojia.soundscape.query.album.TrackInfoQuery;
import com.luojia.soundscape.vo.album.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    /**
     * 保存声音信息
     * @param userId
     * @param trackInfoVo
     */
    void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo);

    /**
     * 保存声音统计信息
     * @param trackId 声音ID
     * @param statType 统计类型
     * @param statNum 统计数值
     */
    void saveTrackStat(Long trackId, String statType, int statNum);

    /**
     * 查询分页查询声音列表（包含统计信息）
     * @param pageInfo MP分页对象
     * @param trackInfoQuery 查询条件
     * @return MP分页对象
     */
    IPage<TrackListVo> findUserTrackPage(IPage<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery);

    /**
     * 更新声音信息
     * @param id 声音ID
     * @param trackInfoVo 声音VO信息
     */
    void updateTrackInfo(Long id, TrackInfoVo trackInfoVo);

    /**
     * 删除声音信息（包括音频文件）
     * @param id 声音ID
     * @return
     */
    void removeTrackInfo(Long id);

    /**
     * 根据专辑ID分页查询声音列表包含统计信息（动态渲染付费标识）
     * @param albumId 专辑ID
     * @param userId 用户ID
     * @return 分页对象
     */
    IPage<AlbumTrackListVo> findAlbumTrackPage(IPage<AlbumTrackListVo> pageInfo, Long albumId, Long userId);

    /**
     * 增量更新声音统计数值
     * @param trackStatMqVo
     */
    void updateTrackStat(TrackStatMqVo trackStatMqVo);

    /**
     * 查询声音统计信息
     * @param trackId
     * @return
     */
    TrackStatVo getTrackStatVo(Long trackId);

    /**
     * 基于用户未购买声音数量动态构建分集购买列表
     *
     * @param userId 用户ID
     * @param trackId 声音ID 选中购买声音ID
     * @return
     */
    List<Map<String, Object>> findUserTrackPaidList(Long trackId, Long userId);

    /**
     * 以提交声音ID作为标准，查询未购买声音列表
     * @param trackId 提交声音ID
     * @param trackCount 声音数量
     * @return 声音列表
     */
    List<TrackInfo> findPaidTrackInfoList(Long trackId, Integer trackCount, Long userId);

    List<TrackBriefVo> getTrackInfoBatch(List<Long> ids);
}
