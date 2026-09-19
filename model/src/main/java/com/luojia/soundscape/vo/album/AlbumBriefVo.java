package com.luojia.soundscape.vo.album;

import lombok.Data;

import java.io.Serializable;

@Data
public class AlbumBriefVo implements Serializable {

    private Long id;
    private String albumTitle;
    private String coverUrl;
    private Integer includeTrackCount;
    private String isFinished;
}
