package com.luojia.soundscape.search.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.extra.pinyin.PinyinUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSON;
import com.luojia.soundscape.album.AlbumFeignClient;
import com.luojia.soundscape.common.cache.SoundscapeCache;
import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.common.rabbit.service.CacheInvalidationService;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.album.*;
import com.luojia.soundscape.model.search.AlbumInfoIndex;
import com.luojia.soundscape.model.search.AttributeValueIndex;
import com.luojia.soundscape.model.search.SuggestIndex;
import com.luojia.soundscape.query.search.AlbumIndexQuery;
import com.luojia.soundscape.search.repository.AlbumInfoIndexRepository;
import com.luojia.soundscape.search.repository.SuggestInfoRepository;
import com.luojia.soundscape.search.service.SearchService;
import com.luojia.soundscape.search.service.RankingSnapshotStore;
import com.luojia.soundscape.user.client.UserFeignClient;
import com.luojia.soundscape.vo.album.AlbumStatVo;
import com.luojia.soundscape.vo.album.TrackStatMqVo;
import com.luojia.soundscape.vo.search.AlbumInfoIndexVo;
import com.luojia.soundscape.vo.search.AlbumSearchResponseVo;
import com.luojia.soundscape.vo.user.UserInfoVo;
import jakarta.annotation.Resource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    /**
     * 期望单例，实际有多个对象
     * 1.Autowired 先按类型注入,再按BeanID注入 将遍历名称改为BeanID
     * 2.@Primary注解 放在优先使用Bean上
     * 3.@Qualifier指定BeanID
     * 4.@Resource先根据BeanID注入，再根据Bean类型注入
     */
    //@Autowired
    //@Qualifier("threadPoolExecutor")
    @Resource(name = "searchIndexExecutor")
    private Executor executor;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private RankingSnapshotStore rankingSnapshotStore;

    @Autowired
    private CacheInvalidationService cacheInvalidationService;

    @Override
    public void saveAlbumInfoIndex(Long albumId) {
        // 1. 异步获取专辑信息（含标签列表）
        CompletableFuture<AlbumInfo> albumFuture = CompletableFuture.supplyAsync(() -> {
            Result<AlbumInfo> result = albumFeignClient.getAlbumInfo(albumId);
            if (result == null || result.getData() == null) {
                log.error("获取专辑信息失败，albumId:{}", albumId);
                return null;
            }
            return result.getData();
        }, executor);

        // 2. 基于专辑信息，并行获取分类视图和主播名称
        CompletableFuture<BaseCategoryView> categoryFuture = albumFuture.thenApplyAsync(albumInfo -> {
            if (albumInfo == null) return null;
            Long c3Id = albumInfo.getCategory3Id();
            if (c3Id == null) return null;
            Result<BaseCategoryView> result = albumFeignClient.getCategoryView(c3Id);
            return (result != null && result.getData() != null) ? result.getData() : null;
        }, executor);

        CompletableFuture<String> announcerFuture = albumFuture.thenApplyAsync(albumInfo -> {
            if (albumInfo == null) return null;
            Long uid = albumInfo.getUserId();
            if (uid == null) return null;
            Result<UserInfoVo> result = userFeignClient.getUserInfoVo(uid);
            return (result != null && result.getData() != null) ? result.getData().getNickname() : null;
        }, executor);

        // 3. 等待所有异步任务完成，汇总结果
        CompletableFuture.allOf(albumFuture, categoryFuture, announcerFuture).orTimeout(1, TimeUnit.SECONDS).join();
        AlbumInfo albumInfo = albumFuture.join();
        if (albumInfo == null) return;
        BaseCategoryView categoryView = categoryFuture.join();
        String announcerName = announcerFuture.join();

        // 4. 转换标签列表 AlbumAttributeValue -> AttributeValueIndex
        List<AttributeValueIndex> attributeValueIndexList = new ArrayList<>();
        List<AlbumAttributeValue> attributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
        if (attributeValueVoList != null && !attributeValueVoList.isEmpty()) {
            for (AlbumAttributeValue attr : attributeValueVoList) {
                AttributeValueIndex attrIndex = new AttributeValueIndex();
                attrIndex.setAttributeId(attr.getAttributeId());
                attrIndex.setValueId(attr.getValueId());
                attributeValueIndexList.add(attrIndex);
            }
        }

        // 5. 生成四项统计随机值
        //ThreadLocalRandom random = ThreadLocalRandom.current();
        //int playStatNum = random.nextInt(500, 10001);       // 播放量 500~10000
        //int subscribeStatNum = random.nextInt(50, 801);     // 订阅量 50~800
        //int buyStatNum = random.nextInt(0, 201);            // 购买量 0~200
        //int commentStatNum = random.nextInt(0, 81);         // 评论数 0~80
        AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
        Assert.notNull(albumStatVo, "获取专辑统计信息失败");
        int playStatNum = albumStatVo.getPlayStatNum();       // 播放量 500~10000
        int subscribeStatNum = albumStatVo.getSubscribeStatNum();     // 订阅量 50~800
        int buyStatNum = albumStatVo.getBuyStatNum();          // 购买量 0~200
        int commentStatNum = albumStatVo.getCommentStatNum();
        // 6. 计算热度值
        // 权重：播放×0.3 + 订阅×0.5 + 购买×0.8 + 评论×1.0
        double hotScore = playStatNum * 0.3 + subscribeStatNum * 0.5 + buyStatNum * 0.8 + commentStatNum * 1.0;

        // 7. 构建AlbumInfoIndex对象
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        albumInfoIndex.setId(albumInfo.getId());
        albumInfoIndex.setAlbumTitle(albumInfo.getAlbumTitle());
        albumInfoIndex.setAlbumIntro(albumInfo.getAlbumIntro());
        albumInfoIndex.setAnnouncerName(announcerName);
        albumInfoIndex.setCoverUrl(albumInfo.getCoverUrl());
        albumInfoIndex.setIncludeTrackCount(albumInfo.getIncludeTrackCount());
        albumInfoIndex.setIsFinished(albumInfo.getIsFinished());
        albumInfoIndex.setPayType(albumInfo.getPayType());
        albumInfoIndex.setCreateTime(albumInfo.getCreateTime());
        albumInfoIndex.setCategory3Id(albumInfo.getCategory3Id());
        if (categoryView != null) {
            albumInfoIndex.setCategory1Id(categoryView.getCategory1Id());
            albumInfoIndex.setCategory2Id(categoryView.getCategory2Id());
        }
        albumInfoIndex.setPlayStatNum(playStatNum);
        albumInfoIndex.setSubscribeStatNum(subscribeStatNum);
        albumInfoIndex.setBuyStatNum(buyStatNum);
        albumInfoIndex.setCommentStatNum(commentStatNum);
        albumInfoIndex.setHotScore(hotScore);
        albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);

        // 8. 保存到ES
        albumInfoIndexRepository.save(albumInfoIndex);
        if (albumInfoIndex.getCategory1Id() != null) {
            cacheInvalidationService.evictAfterCommit(
                    RedisConstant.SEARCH_CHANNEL_PREFIX + albumInfoIndex.getCategory1Id());
        }
        log.info("专辑索引文档保存成功，albumId:{}, hotScore:{}", albumId, hotScore);

        // 9. 将专辑标题保存到"提示词"索引库
        this.saveSuggestInfoIndex(albumInfoIndex);

        //10. 将专辑ID存入布隆过滤器
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        bloomFilter.add(albumId);
    }

    @Autowired
    private RedissonClient redissonClient;

    /**
     * @param query 检索参数
     * @return
     */
    @Override
    @SoundscapeCache(prefix = RedisConstant.SEARCH_QUERY_PREFIX, ttl = 2, staleTtl = 0, l1Ttl = 1)
    public AlbumSearchResponseVo search(AlbumIndexQuery query) {
        try {
            String keyword = query.getKeyword();
            Integer pageNo = query.getPageNo();
            Integer pageSize = query.getPageSize();

            // 解析排序字段和方向: 1=综合(hotScore), 2=播放量(playStatNum), 3=发布时间(createTime)
            String sortField = "hotScore";
            SortOrder sortOrder = SortOrder.Desc;
            String order = query.getOrder();
            if (StringUtils.isNotBlank(order)) {
                String[] parts = order.split(":");
                sortOrder = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1])) ? SortOrder.Asc : SortOrder.Desc;
                switch (parts[0]) {
                    case "1":
                        sortField = "hotScore";
                        break;
                    case "2":
                        sortField = "playStatNum";
                        break;
                    case "3":
                        sortField = "createTime";
                        break;
                }
            }

            final int from = (pageNo - 1) * pageSize;
            final String finalSortField = sortField;
            final SortOrder finalSortOrder = sortOrder;

            SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(s -> s
                            .index("albuminfo")
                            .query(q -> q.bool(b -> {
                                // 关键词: albumTitle + albumIntro (ik_max_word)
                                if (StringUtils.isNotBlank(keyword)) {
                                    b.must(m -> m.match(m1 -> m1.field("albumTitle").query(keyword)));
                                }
                                // 分类过滤 (term精确匹配)
                                if (query.getCategory1Id() != null) {
                                    b.filter(f -> f.term(t -> t.field("category1Id").value(query.getCategory1Id())));
                                }
                                if (query.getCategory2Id() != null) {
                                    b.filter(f -> f.term(t -> t.field("category2Id").value(query.getCategory2Id())));
                                }
                                if (query.getCategory3Id() != null) {
                                    b.filter(f -> f.term(t -> t.field("category3Id").value(query.getCategory3Id())));
                                }
                                // 属性筛选 (nested: 每个attributeId:valueId对独立匹配)
                                if (CollUtil.isNotEmpty(query.getAttributeList())) {
                                    for (String attr : query.getAttributeList()) {
                                        if (StringUtils.isNotBlank(attr) && attr.contains(":")) {
                                            String[] parts = attr.split(":");
                                            Long attrId = Long.valueOf(parts[0]);
                                            Long valueId = Long.valueOf(parts[1]);
                                            b.filter(f -> f.nested(n -> n
                                                    .path("attributeValueIndexList")
                                                    .query(nq -> nq.bool(nb ->
                                                            nb.must(nm -> nm.term(nt -> nt.field("attributeValueIndexList.attributeId").value(attrId)))
                                                                    .must(nm -> nm.term(nt -> nt.field("attributeValueIndexList.valueId").value(valueId)))
                                                    ))
                                            ));
                                        }
                                    }
                                }
                                return b;
                            }))
                            .from(from)
                            .size(pageSize)
                            .sort(so -> so.field(f -> f.field(finalSortField).order(finalSortOrder)))
                            .highlight(h -> h
                                    .fields("albumTitle", hf -> hf.preTags("<font color='red'>").postTags("</font>"))
                            ),
                    AlbumInfoIndex.class
            );

            // 映射结果 -> AlbumInfoIndexVo
            List<AlbumInfoIndexVo> voList = new ArrayList<>();
            List<Hit<AlbumInfoIndex>> hits = response.hits().hits();
            for (Hit<AlbumInfoIndex> hit : hits) {
                AlbumInfoIndex source = hit.source();
                if (source == null) continue;
                AlbumInfoIndexVo vo = BeanUtil.copyProperties(source, AlbumInfoIndexVo.class);
                // 高亮字段替换原值
                Map<String, List<String>> highlights = hit.highlight();
                if (highlights != null) {
                    List<String> titleHighlight = highlights.get("albumTitle");
                    if (CollUtil.isNotEmpty(titleHighlight)) {
                        vo.setAlbumTitle(titleHighlight.get(0));
                    }
                }
                voList.add(vo);
            }

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            long totalPages = (total + pageSize - 1) / pageSize;

            AlbumSearchResponseVo result = new AlbumSearchResponseVo();
            result.setList(voList);
            result.setTotal(total);
            result.setPageSize(pageSize);
            result.setPageNo(pageNo);
            result.setTotalPages(totalPages);
            return result;
        } catch (Exception e) {
            log.error("ES搜索失败，query:{}", query, e);
            throw new RuntimeException("搜索服务异常", e);
        }
    }

    private static final String INDEX_NAME = "albuminfo";

    /**
     * 查询1级分类下置顶分类热门专辑
     *
     * @param category1Id
     * @return
     */
    @Override
    @SoundscapeCache(prefix = RedisConstant.SEARCH_CHANNEL_PREFIX, ttl = 5, staleTtl = 30, l1Ttl = 1)
    public List<Map<String, Object>> channel(Long category1Id) {
        try {
            //1.远程调用专辑服务获取置顶7个三级分类列表
            List<BaseCategory3> baseCategory3List = albumFeignClient.findTop7BaseCategory3(category1Id).getData();
            if (CollUtil.isNotEmpty(baseCategory3List)) {
                //1.1. 得到三级分类ID对应的FieldValue集合
                List<FieldValue> fieldValueList
                        = baseCategory3List.stream().map(c3 -> FieldValue.of(c3.getId())).collect(Collectors.toList());
                //1.2 将三级分类集合转为 Map<三级分类ID，三级分类对象>
                Map<Long, BaseCategory3> category3Map = baseCategory3List.stream()
                        .collect(Collectors.toMap(BaseCategory3::getId, c3 -> c3));
                //2.采用多关键字+聚合查询置顶分类热门专辑
                SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(s -> s.index(INDEX_NAME)
                                .query(q -> q.terms(t -> t.field("category3Id").terms(tq -> tq.value(fieldValueList))))
                                .aggregations("c3_agg", a -> a.terms(t -> t.field("category3Id").size(10))
                                        .aggregations("top6_agg", a1 -> a1.topHits(top -> top.size(6).source(source -> source.filter(f -> f.excludes("attributeValueIndexList", "commentStatNum", "subscribeStatNum", "isFinished"))).sort(s1 -> s1.field(f -> f.field("hotScore").order(SortOrder.Desc)))))
                                )
                                .size(0)
                        , AlbumInfoIndex.class);
                //3.解析聚合结果
                //3.1 获取三级分类聚合结果
                Map<String, Aggregate> aggregations = searchResponse.aggregations();
                Aggregate c3Agg = aggregations.get("c3_agg");
                //3.2 获取聚合到桶列表
                List<LongTermsBucket> bucketList = c3Agg.lterms().buckets().array();
                //3.3 遍历7个桶bucket列表 没遍历一个创建置顶分类热门专辑Map
                List<Map<String, Object>> list = bucketList.stream().map(bucket -> {
                    //3.3.1 创建置顶分类热门专辑Map
                    Map<String, Object> map = new HashMap<>();
                    //3.3.2 获取三级分类ID，封装Map中分类对象
                    map.put("baseCategory3", category3Map.get(bucket.key()));
                    //3.3.3 通过子聚合获取TOP6专辑列表
                    TopHitsAggregate top6Agg = bucket.aggregations().get("top6_agg").topHits();
                    List<AlbumInfoIndex> top6List = top6Agg.hits().hits().stream().map(hit -> {
                        String jsonDataStr = hit.source().toString();
                        return JSON.parseObject(jsonDataStr, AlbumInfoIndex.class);
                    }).collect(Collectors.toList());
                    //3.4 封装Map中热门专辑
                    map.put("list", top6List);
                    //3.5 返回"置顶分类热门专辑Map对象"
                    return map;
                }).collect(Collectors.toList());
                return list;
            }
        } catch (IOException e) {
            log.error("查询1级分类下置顶分类热门专辑失败", e);
            throw new RuntimeException(e);
        }

        return List.of();
    }

    @Autowired
    private SuggestInfoRepository suggestInfoRepository;

    /**
     * 将专辑标题存入提示词索引库
     *
     * @param albumInfoIndex
     */
    @Override
    public void saveSuggestInfoIndex(AlbumInfoIndex albumInfoIndex) {
        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(albumInfoIndex.getId().toString());
        String albumTitle = albumInfoIndex.getAlbumTitle();
        suggestIndex.setTitle(albumTitle);
        suggestIndex.setKeyword(new Completion(new String[]{albumTitle}));
        String pinyin = PinyinUtil.getPinyin(albumTitle, "");
        suggestIndex.setKeywordPinyin(new Completion(new String[]{pinyin}));
        String firstLetter = PinyinUtil.getFirstLetter(albumTitle, "");
        suggestIndex.setKeywordSequence(new Completion(new String[]{firstLetter}));
        suggestInfoRepository.save(suggestIndex);

    }

    @Override
    public void removeAlbumInfoIndex(Long albumId) {
        Long category1Id = albumInfoIndexRepository.findById(albumId)
                .map(AlbumInfoIndex::getCategory1Id)
                .orElse(null);
        albumInfoIndexRepository.deleteById(albumId);
        if (category1Id != null) {
            cacheInvalidationService.evictAfterCommit(RedisConstant.SEARCH_CHANNEL_PREFIX + category1Id);
        }
        log.info("专辑索引文档删除成功，albumId:{}", albumId);
        suggestInfoRepository.deleteById(albumId.toString());
        log.info("专辑建议词文档删除成功，albumId:{}", albumId);
    }

    private static final String SUGGEST_INDEX_NAME = "suggestinfo";

    /**
     * 搜索关键词自动补全
     *
     * @param keyword 用户已录入字符
     * @return ["待选项1","待选项2"]
     */
    @Override
    public List<String> completeSuggest(String keyword) {
        try {
            //1.执行建议词自动补全检索
            SearchResponse<SuggestIndex> searchResponse = elasticsearchClient.search(s -> s.index(SUGGEST_INDEX_NAME)
                            .suggest(
                                    s1 -> s1.suggesters("keyword-suggest", s2 -> s2.prefix(keyword).completion(c -> c.field("keyword").skipDuplicates(true)))
                                            .suggesters("pinyin-suggest", s3 -> s3.prefix(keyword).completion(c -> c.field("keywordPinyin").skipDuplicates(true).fuzzy(f -> f.fuzziness("AUTO"))))
                                            .suggesters("letter-suggest", s3 -> s3.prefix(keyword).completion(c -> c.field("keywordSequence").skipDuplicates(true)))

                            )
                    , SuggestIndex.class);
            //2.解析结果 如果自动补全结果数量不足10个采用全文检索专辑索引库尝试补全到10个
            //2.1 声明set集合，用于去重
            HashSet<String> set = new HashSet<>();
            //2.2 解析建议词响应结果
            set.addAll(parseSuggestResponse("keyword-suggest", searchResponse));
            set.addAll(parseSuggestResponse("pinyin-suggest", searchResponse));
            set.addAll(parseSuggestResponse("letter-suggest", searchResponse));
            //2.3 判断数量如果小于10个采用全文检索专辑索引库补全
            if (set.size() < 10) {
                SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(s -> s.index(INDEX_NAME)
                                .query(q -> q.match(m -> m.field("albumTitle").query(keyword)))
                                .size(10)
                                .source(s1 -> s1.filter(f -> f.includes("albumTitle")))
                        , AlbumInfoIndex.class);
                List<Hit<AlbumInfoIndex>> hits = response.hits().hits();
                if (CollUtil.isNotEmpty(hits)) {
                    for (Hit<AlbumInfoIndex> hit : hits) {
                        String albumTitle = hit.source().getAlbumTitle();
                        set.add(albumTitle);
                        if (set.size() >= 10) {
                            break;
                        }
                    }
                }
            }
            //2.4 响应自动补全结果
            if (set.size() > 10) {
                return new ArrayList<>(set).subList(0, 10);
            }
            return new ArrayList<>(set);
        } catch (IOException e) {
            log.error("关键词自动补全失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     #增量更新
     POST /albuminfo/_update/1
     {
     "script": {
     "source": "ctx._source.playStatNum += params.increment",
     "lang": "painless",
     "params": {
     "increment": 1
     }
     }
     }
     */
    /**
     * 增量更新ES文档统计数值
     *
     * @param mqVo
     */
    @Override
    public void updateAlbumStat(TrackStatMqVo mqVo) {
        try {
            Long albumId = mqVo.getAlbumId();
            String incrementField = "";
            if (SystemConstant.TRACK_STAT_PLAY.equals(mqVo.getStatType())) {
                incrementField = "playStatNum";

            } else if (SystemConstant.TRACK_STAT_COMMENT.equals(mqVo.getStatType())) {
                incrementField = "commentStatNum";
            }
            String finalIncrementField = incrementField;
            elasticsearchClient.update(
                    u -> u.index(INDEX_NAME).id(albumId.toString())
                            .script(s -> s.inline(i -> i.source("ctx._source." + finalIncrementField + " += params.increment").lang("painless").params(Map.of("increment", JsonData.of(mqVo.getCount()))))),
                    AlbumInfoIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 更新Redis小时榜TOPN记录
     * 从ES查询获取，存入Redis中Hash结构中
     *
     * @param topN
     */
    @Override
    public void updateLatelyAlbumRanking(Integer topN) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            int safeTopN = Math.max(1, Math.min(topN == null ? 10 : topN, 100));
            List<String> dimensions = List.of("hotScore", "playStatNum", "subscribeStatNum",
                    "buyStatNum", "commentStatNum");
            SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(search -> {
                search.index(INDEX_NAME).size(0);
                search.aggregations("categories", categories -> categories
                        .terms(terms -> terms.field("category1Id").size(1000))
                        .aggregations(buildRankingAggregations(dimensions, safeTopN)));
                return search;
            }, AlbumInfoIndex.class);

            Aggregate categoryAggregate = response.aggregations().get("categories");
            Map<String, Object> snapshot = new HashMap<>();
            if (categoryAggregate != null && categoryAggregate.isLterms()) {
                for (LongTermsBucket bucket : categoryAggregate.lterms().buckets().array()) {
                    long categoryId = bucket.key();
                    for (String dimension : dimensions) {
                        Aggregate aggregate = bucket.aggregations().get(dimension);
                        if (aggregate == null || !aggregate.isTopHits()) {
                            continue;
                        }
                        List<AlbumInfoIndex> albums = aggregate.topHits().hits().hits().stream()
                                .filter(hit -> hit.source() != null)
                                .map(hit -> JSON.parseObject(hit.source().toJson().toString(), AlbumInfoIndex.class))
                                .toList();
                        snapshot.put(categoryId + ":" + dimension, albums);
                    }
                }
            }

            rankingSnapshotStore.publish(snapshot);
        } catch (IOException e) {
            log.error("更新Redis小时榜TOPN记录失败", e);
            throw new RuntimeException(e);
        } finally {
            sample.stop(Timer.builder("luojia-soundscape.ranking.refresh")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    private Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregation> buildRankingAggregations(
            List<String> dimensions, int topN) {
        Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregation> aggregations = new HashMap<>();
        for (String dimension : dimensions) {
            aggregations.put(dimension, co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                    .topHits(topHits -> topHits.size(topN)
                            .sort(sort -> sort.field(field -> field.field(dimension).order(SortOrder.Desc)))
                            .source(source -> source.filter(filter -> filter.includes("id", "albumTitle",
                                    "albumIntro", "coverUrl", "payType", "includeTrackCount", "playStatNum"))))));
        }
        return aggregations;
    }

    /**
     * 查询小时榜TOPN记录
     *
     * @param category1Id
     * @param dimension
     * @return
     */
    @Override
    public List<AlbumInfoIndex> findRankingList(Long category1Id, String dimension) {
        List<AlbumInfoIndex> snapshot = rankingSnapshotStore.read(category1Id, dimension);
        if (snapshot != null) {
            return snapshot;
        }
        //声明小时榜Hash结构Key 形式=前缀+1级分类ID
        String key = RedisConstant.RANKING_KEY_PREFIX + category1Id;
        // 创建hash操作对象
        BoundHashOperations<String, String, List<AlbumInfoIndex>> hashOps = redisTemplate.boundHashOps(key);
        //判断Hash中field是否存在
        //if (redisTemplate.opsForHash().hasKey(key, dimension)) {
        //    List<AlbumInfoIndex> list = (List<AlbumInfoIndex>) redisTemplate.opsForHash().get(key, dimension);
        //    return list;
        //}
        if(hashOps.hasKey(dimension)){
            return hashOps.get(dimension);
        }
        return null;
    }

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 通过自定义建议器名称，从响应结果中获取建议词
     *
     * @param suggest_name   建议器名称
     * @param searchResponse ES检索结果
     * @return ["待选项1","待选项2"]
     */
    private Collection<String> parseSuggestResponse(String suggest_name, SearchResponse<SuggestIndex> searchResponse) {
        ArrayList<String> list = new ArrayList<>();
        //1.获取建议补全结果对象
        Map<String, List<Suggestion<SuggestIndex>>> suggest = searchResponse.suggest();
        //2.根据自定义建议器名称获取建议补全结果
        List<Suggestion<SuggestIndex>> suggestions = suggest.get(suggest_name);
        //3.遍历将符合要求标题添加到set集合中
        for (Suggestion<SuggestIndex> suggestion : suggestions) {
            for (CompletionSuggestOption<SuggestIndex> option : suggestion.completion().options()) {
                String title = option.source().getTitle();
                list.add(title);
            }
        }
        return list;
    }


}
