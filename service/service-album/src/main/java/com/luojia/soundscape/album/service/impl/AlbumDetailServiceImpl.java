package com.luojia.soundscape.album.service.impl;

import com.luojia.soundscape.album.service.AlbumDetailService;
import com.luojia.soundscape.album.service.AlbumInfoService;
import com.luojia.soundscape.album.service.BaseCategoryService;
import com.luojia.soundscape.common.cache.SoundscapeCache;
import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.common.execption.SoundscapeException;
import com.luojia.soundscape.model.album.AlbumInfo;
import com.luojia.soundscape.vo.album.AlbumDetailVo;
import org.springframework.stereotype.Service;

@Service
public class AlbumDetailServiceImpl implements AlbumDetailService {

    private final AlbumInfoService albumInfoService;
    private final BaseCategoryService baseCategoryService;

    public AlbumDetailServiceImpl(AlbumInfoService albumInfoService, BaseCategoryService baseCategoryService) {
        this.albumInfoService = albumInfoService;
        this.baseCategoryService = baseCategoryService;
    }

    @Override
    @SoundscapeCache(prefix = RedisConstant.ALBUM_DETAIL_PREFIX, ttl = 3, staleTtl = 30, l1Ttl = 1)
    public AlbumDetailVo getAlbumDetail(Long albumId) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfoFromDB(albumId);
        if (albumInfo == null) {
            throw new SoundscapeException(404, "专辑不存在");
        }
        AlbumDetailVo detail = new AlbumDetailVo();
        detail.setAlbumInfo(albumInfo);
        detail.setAlbumStatVo(albumInfoService.getAlbumStatVo(albumId));
        detail.setBaseCategoryView(baseCategoryService.getCategoryView(albumInfo.getCategory3Id()));
        return detail;
    }
}
