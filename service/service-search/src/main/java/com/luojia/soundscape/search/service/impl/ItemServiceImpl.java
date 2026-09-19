package com.luojia.soundscape.search.service.impl;

import com.luojia.soundscape.album.AlbumFeignClient;
import com.luojia.soundscape.common.cache.SoundscapeCache;
import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.common.execption.SoundscapeException;
import com.luojia.soundscape.search.service.ItemService;
import com.luojia.soundscape.user.client.UserFeignClient;
import com.luojia.soundscape.vo.album.AlbumDetailVo;
import com.luojia.soundscape.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 查询专辑详情-汇总详情页渲染所需参数
     *
     * @param albumId
     * @return {announcer:主播信息,albumInfo:专辑对象,albumStatVo:统计对象, baseCategoryView:分类对象}
     */
    @Override
    @SoundscapeCache(prefix = RedisConstant.SEARCH_ITEM_PREFIX, ttl = 3, staleTtl = 30, l1Ttl = 1)
    public Map<String, Object> item(Long albumId) {
        //0. 基于布隆过滤器判断专辑是否存在 不存在则返回异常
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        if (!bloomFilter.contains(albumId)) {
            throw new SoundscapeException(404, "专辑不存在");
        }

        AlbumDetailVo detail = albumFeignClient.getAlbumDetail(albumId).getData();
        if (detail == null || detail.getAlbumInfo() == null) {
            throw new SoundscapeException(503, "专辑详情服务暂时不可用");
        }
        UserInfoVo announcer = userFeignClient.getUserInfoVo(detail.getAlbumInfo().getUserId()).getData();
        if (announcer == null) {
            throw new SoundscapeException(503, "主播服务暂时不可用");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("albumInfo", detail.getAlbumInfo());
        map.put("albumStatVo", detail.getAlbumStatVo());
        map.put("baseCategoryView", detail.getBaseCategoryView());
        map.put("announcer", announcer);
        return map;

    }
}
