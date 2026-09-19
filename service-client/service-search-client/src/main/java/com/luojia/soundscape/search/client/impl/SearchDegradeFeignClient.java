package com.luojia.soundscape.search.client.impl;

import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.search.client.SearchFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Luojia Soundscape Contributors
 */

@Slf4j
@Component
public class SearchDegradeFeignClient implements SearchFeignClient {

    @Override
    public Result updateLatelyAlbumRanking(Integer topN) {
        log.error("搜索服务调用失败，无法更新排行榜，topN={}", topN);
        return Result.fail().message("搜索服务不可用");
    }
}
