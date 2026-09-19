package com.luojia.soundscape.user.service.impl;

import com.luojia.soundscape.album.AlbumFeignClient;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.model.user.UserSubscribe;
import com.luojia.soundscape.vo.album.AlbumBriefVo;
import com.luojia.soundscape.vo.user.UserSubscribeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserInfoServiceBatchTest {

    @Test
    void subscriptionPageUsesOneBatchFeignCallAndKeepsMongoOrder() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        AlbumFeignClient albumFeignClient = mock(AlbumFeignClient.class);
        UserInfoServiceImpl service = new UserInfoServiceImpl();
        ReflectionTestUtils.setField(service, "mongoTemplate", mongoTemplate);
        ReflectionTestUtils.setField(service, "albumFeignClient", albumFeignClient);

        List<UserSubscribe> subscriptions = new ArrayList<>();
        List<AlbumBriefVo> albums = new ArrayList<>();
        for (long id = 20; id >= 1; id--) {
            UserSubscribe subscription = new UserSubscribe();
            subscription.setUserId(1L);
            subscription.setAlbumId(id);
            subscription.setCreateTime(new Date());
            subscriptions.add(subscription);

            AlbumBriefVo album = new AlbumBriefVo();
            album.setId(id);
            album.setAlbumTitle("album-" + id);
            albums.add(album);
        }
        when(mongoTemplate.count(any(Query.class), eq(UserSubscribe.class), anyString())).thenReturn(20L);
        when(mongoTemplate.find(any(Query.class), eq(UserSubscribe.class), anyString())).thenReturn(subscriptions);
        when(albumFeignClient.getAlbumInfoBatch(any())).thenReturn(Result.ok(albums));

        IPage<UserSubscribeVo> page = service.findUserSubscribePage(1L, 1, 20);

        assertThat(page.getRecords()).extracting(UserSubscribeVo::getAlbumId)
                .containsExactlyElementsOf(subscriptions.stream().map(UserSubscribe::getAlbumId).toList());
        verify(albumFeignClient, times(1)).getAlbumInfoBatch(any());
        verify(albumFeignClient, never()).getAlbumInfo(anyLong());
    }
}
