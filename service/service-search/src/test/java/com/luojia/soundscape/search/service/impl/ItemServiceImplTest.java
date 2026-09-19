package com.luojia.soundscape.search.service.impl;

import com.luojia.soundscape.album.AlbumFeignClient;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.album.AlbumInfo;
import com.luojia.soundscape.model.album.BaseCategoryView;
import com.luojia.soundscape.user.client.UserFeignClient;
import com.luojia.soundscape.vo.album.AlbumDetailVo;
import com.luojia.soundscape.vo.album.AlbumStatVo;
import com.luojia.soundscape.vo.user.UserInfoVo;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ItemServiceImplTest {

    @Test
    void aggregateEndpointKeepsTheFourLegacyResponseKeys() {
        AlbumFeignClient albumClient = mock(AlbumFeignClient.class);
        UserFeignClient userClient = mock(UserFeignClient.class);
        RedissonClient redisson = mock(RedissonClient.class);
        RBloomFilter<Long> bloom = mock(RBloomFilter.class);
        when(redisson.<Long>getBloomFilter(anyString())).thenReturn(bloom);
        when(bloom.contains(1L)).thenReturn(true);

        AlbumInfo album = new AlbumInfo();
        album.setId(1L);
        album.setUserId(7L);
        AlbumDetailVo detail = new AlbumDetailVo();
        detail.setAlbumInfo(album);
        detail.setAlbumStatVo(new AlbumStatVo());
        detail.setBaseCategoryView(new BaseCategoryView());
        when(albumClient.getAlbumDetail(1L)).thenReturn(Result.ok(detail));
        when(userClient.getUserInfoVo(7L)).thenReturn(Result.ok(new UserInfoVo()));

        ItemServiceImpl service = new ItemServiceImpl();
        ReflectionTestUtils.setField(service, "albumFeignClient", albumClient);
        ReflectionTestUtils.setField(service, "userFeignClient", userClient);
        ReflectionTestUtils.setField(service, "redissonClient", redisson);

        Map<String, Object> response = service.item(1L);

        assertThat(response).containsOnlyKeys("albumInfo", "albumStatVo", "baseCategoryView", "announcer");
        verify(albumClient, times(1)).getAlbumDetail(1L);
        verify(albumClient, never()).getAlbumInfo(anyLong());
        verify(albumClient, never()).getAlbumStatVo(anyLong());
        verify(albumClient, never()).getCategoryView(anyLong());
    }
}
