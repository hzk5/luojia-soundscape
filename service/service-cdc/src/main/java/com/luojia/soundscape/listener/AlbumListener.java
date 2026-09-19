package com.luojia.soundscape.listener;

import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.model.album.AlbumInfo;
import com.luojia.soundscape.model.user.UserInfo;
import io.xzxj.canal.core.annotation.CanalListener;
import io.xzxj.canal.core.listener.EntryListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

/**
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@CanalListener(destination = "luojiaSoundscapeTopic", schemaName = "luojia_soundscape_album", tableName = "album_info")
public class AlbumListener implements EntryListener<AlbumInfo> {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 监听用户表更新回调方法
     * @param before
     * @param after
     * @param fields
     */
    @Override
    public void update(AlbumInfo before, AlbumInfo after, Set<String> fields) {
        log.info("[cdc]监听到变更数据");
        String redisKey = RedisConstant.ALBUM_INFO_PREFIX +after.getId();
        redisTemplate.delete(redisKey);
    }
}
