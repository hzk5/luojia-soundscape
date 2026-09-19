package com.luojia.soundscape.vo.album;

import com.luojia.soundscape.model.album.AlbumInfo;
import com.luojia.soundscape.model.album.BaseCategoryView;
import lombok.Data;

import java.io.Serializable;

@Data
public class AlbumDetailVo implements Serializable {

    private AlbumInfo albumInfo;
    private AlbumStatVo albumStatVo;
    private BaseCategoryView baseCategoryView;
}
