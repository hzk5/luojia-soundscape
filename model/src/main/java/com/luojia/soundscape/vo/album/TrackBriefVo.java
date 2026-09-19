package com.luojia.soundscape.vo.album;

import lombok.Data;

import java.io.Serializable;

@Data
public class TrackBriefVo implements Serializable {

    private Long id;
    private Long albumId;
    private String trackTitle;
    private String coverUrl;
}
