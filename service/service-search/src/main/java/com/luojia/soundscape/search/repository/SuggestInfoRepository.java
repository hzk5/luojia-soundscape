package com.luojia.soundscape.search.repository;

import com.luojia.soundscape.model.search.SuggestIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SuggestInfoRepository extends ElasticsearchRepository<SuggestIndex, String> {
}
