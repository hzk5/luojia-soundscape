package com.luojia.soundscape.search.service;

import com.luojia.soundscape.model.search.AlbumInfoIndex;
import com.luojia.soundscape.query.search.AlbumIndexQuery;
import com.luojia.soundscape.vo.album.TrackStatMqVo;
import com.luojia.soundscape.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;

public interface SearchService {

    /**
     * 保存专辑索引库文档到ES
     * @param albumId 专辑ID
     */
    void saveAlbumInfoIndex(Long albumId);

    /**
     * 从ES删除专辑索引库文档
     * @param albumId 专辑ID
     */
    void removeAlbumInfoIndex(Long albumId);

    /**
     * 专辑检索（关键词、分类过滤、属性筛选、排序、分页、高亮）
     * @param query 检索参数
     * @return 检索结果
     */
    AlbumSearchResponseVo search(AlbumIndexQuery query);

    /**
     * 查询1级分类下置顶分类热门专辑
     * @param category1Id
     * @return
     */
    List<Map<String, Object>> channel(Long category1Id);

    /**
     * 将专辑标题存入提示词索引库
     * @param albumInfoIndex
     */
    void saveSuggestInfoIndex(AlbumInfoIndex albumInfoIndex);

    /**
     * 搜索关键词自动补全
     * @param keyword 用户已录入字符
     * @return ["待选项1","待选项2"]
     */
    List<String> completeSuggest(String keyword);

    /**
     * 增量更新ES文档统计数值
     * @param mqVo
     */
    void updateAlbumStat(TrackStatMqVo mqVo);

    /**
     * 更新Redis小时榜TOPN记录
     * @param topN
     */
    void updateLatelyAlbumRanking(Integer topN);

    /**
     * 查询小时榜TOPN记录
     * @param category1Id
     * @param dimension
     * @return
     */
    List<AlbumInfoIndex> findRankingList(Long category1Id, String dimension);
}
