package com.luojia.soundscape.search.service.impl;

import com.luojia.soundscape.common.cache.SoundscapeCache;
import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.query.search.AlbumIndexQuery;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SearchCachePolicyTest {

    @Test
    void searchUsesShortFreshCacheWithoutServingStaleResults() throws Exception {
        Method search = SearchServiceImpl.class.getMethod("search", AlbumIndexQuery.class);
        SoundscapeCache policy = search.getAnnotation(SoundscapeCache.class);

        assertThat(policy).isNotNull();
        assertThat(policy.prefix()).isEqualTo(RedisConstant.SEARCH_QUERY_PREFIX);
        assertThat(policy.ttl()).isEqualTo(2);
        assertThat(policy.l1Ttl()).isEqualTo(1);
        assertThat(policy.staleTtl()).isZero();
    }
}
