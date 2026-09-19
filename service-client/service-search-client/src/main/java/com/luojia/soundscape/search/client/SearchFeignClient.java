package com.luojia.soundscape.search.client;

import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.search.client.impl.SearchDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 搜索模块远程调用API接口
 * </p>
 *
 * @author Luojia Soundscape Contributors
 */
@FeignClient(value = "luojia-soundscape-search", path = "api/search", fallback = SearchDegradeFeignClient.class)
public interface SearchFeignClient {

    /** 定时从 Elasticsearch 生成首页排行榜并写入 Redis。 */
    @GetMapping("/albumInfo/updateLatelyAlbumRanking/{topN}")
    Result updateLatelyAlbumRanking(@PathVariable Integer topN);
}
