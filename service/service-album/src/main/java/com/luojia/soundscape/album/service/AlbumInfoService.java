package com.luojia.soundscape.album.service;

import com.luojia.soundscape.model.album.AlbumInfo;
import com.luojia.soundscape.query.album.AlbumInfoQuery;
import com.luojia.soundscape.vo.album.AlbumInfoVo;
import com.luojia.soundscape.vo.album.AlbumBriefVo;
import com.luojia.soundscape.vo.album.AlbumListVo;
import com.luojia.soundscape.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {


    /***
     * 保存专辑信息
     * @param albumInfoVo 专辑VO信息
     * @param userId 用户ID
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId);

    /**
     * 保存专辑统计信息
     * @param albumId 专辑ID
     * @param statType 统计类型
     * @param statNum 统计数值 0401-播放量 0402-订阅量 0403-购买量 0403-评论数'
     */
    void saveAlbumInfoStat(Long albumId, String statType, int statNum);

    /**
     * 查看当前用户专辑分页列表（包含统计信息）
     * @param pageInfo MP分页对象
     * @param query 查询条件
     * @return MP分页对象
     */
    IPage<AlbumListVo> findUserAlbumPage(IPage<AlbumListVo> pageInfo, AlbumInfoQuery query);

    /**
     * 删除专辑
     * @param id 专辑ID
     */
    void removeAlbumInfo(Long id);

    /**
     * 根据专辑ID查询专辑信息（包含标签列表）
     * @param id 专辑ID
     * @return
     */
    AlbumInfo getAlbumInfo(Long id);
    AlbumInfo getAlbumInfoFromDB(Long id);

    /**
     * 更新专辑信息
     * @param id 专辑ID
     * @param albumInfoVo 专辑VO
     * @return
     */
    void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo);

    /**
     * 查询指定用户专辑列表
     * @param userId
     * @return
     */
    List<AlbumInfo> findUserAllAlbumList(Long userId);

    /**
     * 根据专辑ID查询统计信息
     * @param albumId
     * @return
     */
    AlbumStatVo getAlbumStatVo(Long albumId);

    /**
     * 重建布隆过滤器
     */
    void rebildBloomFilter();

    List<AlbumBriefVo> getAlbumInfoBatch(List<Long> ids);
}
