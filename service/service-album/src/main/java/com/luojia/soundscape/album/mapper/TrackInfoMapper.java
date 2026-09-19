package com.luojia.soundscape.album.mapper;

import com.luojia.soundscape.model.album.TrackInfo;
import com.luojia.soundscape.query.album.TrackInfoQuery;
import com.luojia.soundscape.vo.album.AlbumTrackListVo;
import com.luojia.soundscape.vo.album.TrackListVo;
import com.luojia.soundscape.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {


    /**
     * 查询分页查询声音列表（包含统计信息）
     * @param pageInfo MP分页对象
     * @param trackInfoQuery 查询条件
     * @return MP分页对象
     */
    IPage<TrackListVo> findUserTrackPage(IPage<TrackListVo> pageInfo, @Param("vo") TrackInfoQuery trackInfoQuery);

    /**
     * 根据专辑ID分页查询声音列表包含统计信息（动态渲染付费标识）
     * @param pageInfo 分页对象
     * @param albumId 专辑ID
     * @return 分页对象
     */
    IPage<AlbumTrackListVo> findAlbumTrackPage(IPage<AlbumTrackListVo> pageInfo, @Param("albumId") Long albumId);

    /**
     * 根据声音ID查询声音统计信息
     * @param trackId
     * @return
     */
    TrackStatVo getTrackStatVo(@Param("trackId") Long trackId);
}
