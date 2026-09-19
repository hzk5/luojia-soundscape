package com.luojia.soundscape.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import com.luojia.soundscape.album.mapper.AlbumInfoMapper;
import com.luojia.soundscape.album.mapper.AlbumStatMapper;
import com.luojia.soundscape.album.mapper.TrackInfoMapper;
import com.luojia.soundscape.album.mapper.TrackStatMapper;
import com.luojia.soundscape.album.service.AuditService;
import com.luojia.soundscape.album.service.TrackInfoService;
import com.luojia.soundscape.album.service.VodService;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.common.rabbit.service.CacheInvalidationService;
import com.luojia.soundscape.model.album.AlbumInfo;
import com.luojia.soundscape.model.album.AlbumStat;
import com.luojia.soundscape.model.album.TrackInfo;
import com.luojia.soundscape.model.album.TrackStat;
import com.luojia.soundscape.query.album.TrackInfoQuery;
import com.luojia.soundscape.user.client.UserFeignClient;
import com.luojia.soundscape.vo.album.*;
import com.luojia.soundscape.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.luojia.soundscape.common.constant.SystemConstant.*;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private VodService vodService;

    @Autowired
    private AuditService auditService;

    /** 本地开发默认关闭腾讯云审核；上线时通过 Nacos 开启。 */
    @Value("${luojia-soundscape.audit.enabled:false}")
    private boolean auditEnabled;

    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Autowired
    private CacheInvalidationService cacheInvalidationService;

    @Override
    public List<TrackBriefVo> getTrackInfoBatch(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().limit(100).toList();
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        return trackInfoMapper.selectList(new LambdaQueryWrapper<TrackInfo>()
                        .in(TrackInfo::getId, distinctIds)
                        .select(TrackInfo::getId, TrackInfo::getAlbumId, TrackInfo::getTrackTitle,
                                TrackInfo::getCoverUrl))
                .stream().map(track -> BeanUtil.copyProperties(track, TrackBriefVo.class)).toList();
    }

    /**
     * 保存声音信息
     *
     * @param userId
     * @param trackInfoVo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo) {
        //1.根据所属专辑ID查询专辑信息，得到现有声音数量
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
        Integer includeTrackCount = albumInfo.getIncludeTrackCount();
        //2.保存声音信息
        TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
        //2.1 封装声音属性 用户ID、序号、封面图片、审核状态
        trackInfo.setUserId(userId);
        trackInfo.setOrderNum(includeTrackCount + 1);
        trackInfo.setSource(SystemConstant.TRACK_SOURCE_USER);
        if (StringUtils.isBlank(trackInfoVo.getCoverUrl())) {
            trackInfo.setCoverUrl(albumInfo.getCoverUrl());
        }
        trackInfo.setStatus(auditEnabled ? SystemConstant.TRACK_STATUS_NO_PASS : SystemConstant.TRACK_STATUS_PASS);
        //2.2 调用云点播平台获取音频：时长、大小、类型
        TrackMediaInfoVo trackMediaInfoVo = vodService.getMediaInfo(trackInfo.getMediaFileId());
        if (trackMediaInfoVo != null) {
            trackInfo.setMediaDuration(BigDecimal.valueOf(trackMediaInfoVo.getDuration()));
            trackInfo.setMediaSize(trackMediaInfoVo.getSize());
            trackInfo.setMediaType(trackMediaInfoVo.getType());
        }
        //2.3 保存声音信息,得到声音ID
        trackInfoMapper.insert(trackInfo);
        Long trackId = trackInfo.getId();

        //3.更新专辑信息：声音数量
        AlbumInfo albumInfo_update = new AlbumInfo();
        albumInfo_update.setIncludeTrackCount(includeTrackCount + 1);
        albumInfo_update.setId(albumInfo.getId());
        albumInfoMapper.updateById(albumInfo_update);

        //4.新增声音统计信息：播放 点赞 收藏 评论
        this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_PLAY, 0);
        this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_COLLECT, 0);
        this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_PRAISE, 0);
        this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_COMMENT, 0);

        //5.生产环境开启审核时，才调用腾讯云文本与音视频审核。
        if (auditEnabled) {
            String text = trackInfo.getTrackTitle() + trackInfo.getTrackIntro();
            String suggestion = auditService.audit_text(text);
            if (StringUtils.isNotBlank(suggestion)) {
                if ("block".equals(suggestion)) {
                    trackInfo.setStatus(TRACK_STATUS_NO_PASS);
                } else if ("review".equals(suggestion)) {
                    trackInfo.setStatus(TRACK_STATUS_MANUAL);
                } else if ("pass".equals(suggestion)) {
                    trackInfo.setStatus(TRACK_STATUS_PASS);
                    String taskId = auditService.startReviewTask(trackInfo.getMediaFileId());
                    trackInfo.setStatus(TRACK_STATUS_REVIEWING);
                    trackInfo.setReviewTaskId(taskId);
                }
                trackInfoMapper.updateById(trackInfo);
            }
        }
        cacheInvalidationService.evictAfterCommit(RedisConstant.ALBUM_INFO_PREFIX + trackInfoVo.getAlbumId());
        cacheInvalidationService.evictAfterCommit(RedisConstant.ALBUM_DETAIL_PREFIX + trackInfoVo.getAlbumId());
    }

    @Autowired
    private TrackStatMapper trackStatMapper;

    /**
     * 保存声音统计信息
     *
     * @param trackId  声音ID
     * @param statType 统计类型
     * @param statNum  统计数值
     */
    @Override
    public void saveTrackStat(Long trackId, String statType, int statNum) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statType);
        trackStat.setStatNum(statNum);
        trackStatMapper.insert(trackStat);
    }

    /**
     * 查询分页查询声音列表（包含统计信息）
     *
     * @param pageInfo       MP分页对象
     * @param trackInfoQuery 查询条件
     * @return MP分页对象
     */
    @Override
    public IPage<TrackListVo> findUserTrackPage(IPage<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
        return trackInfoMapper.findUserTrackPage(pageInfo, trackInfoQuery);
    }

    /**
     * 更新声音信息
     *
     * @param id          声音ID
     * @param trackInfoVo 声音VO信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrackInfo(Long id, TrackInfoVo trackInfoVo) {
        //1.根据声音ID查询声音记录 得到 原来音频唯一标识
        TrackInfo trackInfo = trackInfoMapper.selectById(id);
        String oldMediaFileId = trackInfo.getMediaFileId();

        //将vo修改信息拷贝到trackInfo中
        BeanUtil.copyProperties(trackInfoVo, trackInfo);

        if (auditEnabled) {
            String text = trackInfo.getTrackTitle() + trackInfo.getTrackIntro();
            String suggestion = auditService.audit_text(text);
            if (StringUtils.isNotBlank(suggestion)) {
                if ("block".equals(suggestion)) {
                    trackInfo.setStatus(TRACK_STATUS_NO_PASS);
                } else if ("review".equals(suggestion)) {
                    trackInfo.setStatus(TRACK_STATUS_MANUAL);
                } else if ("pass".equals(suggestion)) {
                    trackInfo.setStatus(TRACK_STATUS_PASS);
                }
            }
        } else {
            trackInfo.setStatus(TRACK_STATUS_PASS);
        }

        //2.判断音频是否修改
        if (!Objects.equals(oldMediaFileId, trackInfoVo.getMediaFileId())) {
            //2.1 说明音频文件更新了
            TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
            if (mediaInfo != null) {
                trackInfo.setMediaFileId(trackInfoVo.getMediaFileId());
                trackInfo.setMediaUrl(trackInfoVo.getMediaUrl());
                trackInfo.setMediaType(mediaInfo.getType());
                trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfo.getDuration()));
                trackInfo.setMediaSize(mediaInfo.getSize());

                if (auditEnabled) {
                    String taskId = auditService.startReviewTask(trackInfo.getMediaFileId());
                    trackInfo.setStatus(TRACK_STATUS_REVIEWING);
                    trackInfo.setReviewTaskId(taskId);
                }
            }
            //2.2 删除原来的音频文件
            vodService.deleteMedia(oldMediaFileId);
            //2.3 TODO 如果音频文件更新了，对新音频再次进行审核
        }
        //3.更新声音信息
        trackInfoMapper.updateById(trackInfo);

    }

    /**
     * 删除声音信息（包括音频文件）
     *
     * @param id 声音ID
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTrackInfo(Long id) {
        //1.获取被删除声音记录 得到：音频唯一标识（用于删除点播平台）、声音序号（用于更新其他声音序号）
        TrackInfo trackInfo = trackInfoMapper.selectById(id);
        Integer orderNum = trackInfo.getOrderNum();
        String mediaFileId = trackInfo.getMediaFileId();
        Long albumId = trackInfo.getAlbumId();

        //2.删除声音记录，同时更新专辑包含声音数量
        trackInfoMapper.deleteById(id);
        albumInfoMapper.update(
                null,
                new LambdaUpdateWrapper<AlbumInfo>().eq(AlbumInfo::getId, albumId)
                        .setSql("include_track_count = include_track_count -1")
        );

        //3.更新其他声音序号，确保声音序号连续
        trackInfoMapper.update(
                null,
                new LambdaUpdateWrapper<TrackInfo>().eq(TrackInfo::getAlbumId, albumId)
                        .gt(TrackInfo::getOrderNum, orderNum)
                        .setSql("order_num = order_num -1")
        );

        //4.删除声音统计信息
        trackStatMapper.delete(
                new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, id)
        );

        //5.从点播平台删除文件
        vodService.deleteMedia(mediaFileId);
        cacheInvalidationService.evictAfterCommit(RedisConstant.ALBUM_INFO_PREFIX + albumId);
        cacheInvalidationService.evictAfterCommit(RedisConstant.ALBUM_DETAIL_PREFIX + albumId);
    }

    @Autowired
    private UserFeignClient userFeignClient;

    /**
     * 根据专辑ID分页查询声音列表包含统计信息（动态渲染付费标识）
     *
     * @param albumId 专辑ID
     * @param userId  用户ID
     * @return 分页对象
     */
    @Override
    public IPage<AlbumTrackListVo> findAlbumTrackPage(IPage<AlbumTrackListVo> pageInfo, Long albumId, Long userId) {
        //1.调用持久层执行动态SQL查询声音列表-付费标识都为:false
        pageInfo = trackInfoMapper.findAlbumTrackPage(pageInfo, albumId);
        //2. 根据专辑ID查询付费类型
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        //付费类型: 0101-免费、0102-vip免费、0103-付费
        String payType = albumInfo.getPayType();
        //免费试听集数
        Integer tracksForFree = albumInfo.getTracksForFree();
        // 基于登录状态、用户身份、用户购买情况，动态修改付费标识
        //3.处理未登录情形
        if (userId == null) {
            //3.1 付费类型是VIP免费或付费
            if (ALBUM_PAY_TYPE_VIPFREE.equals(payType) | ALBUM_PAY_TYPE_REQUIRE.equals(payType)) {
                //3.2 除了 试听以外 其他声音都应将付费标识改为true
                pageInfo.getRecords().stream()
                        .filter(track -> track.getOrderNum() > tracksForFree)
                        .forEach(track -> track.setIsShowPaidMark(true));
            }
        } else {
            //4. 处理已登录情形
            //4.1 远程调用"用户服务"获取用户基本信息 得到身份信息
            Boolean isVIP = false;
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
            Assert.notNull(userInfoVo, "用户{}不存在", userId);
            if (userInfoVo.getIsVip().intValue() == 1
                    && userInfoVo.getVipExpireTime().after(new Date())) {
                //会员标识为1，且会员过期时间晚于当前时间
                isVIP = true;
            }

            //4.2 是否需要进一步检查声音购买状态
            Boolean isNeedCheckPayStatus = false;

            //4.2.1 如果是 普通用户 查看 付费类型为：“VIP免费”专辑 默认无权益播放
            if (!isVIP && ALBUM_PAY_TYPE_VIPFREE.equals(payType)) {
                isNeedCheckPayStatus = true;
            }
            //4.2.2 如果是 付费类型为：“付费” 所有用户默认无权益播放
            if (ALBUM_PAY_TYPE_REQUIRE.equals(payType)) {
                isNeedCheckPayStatus = true;
            }

            //4.3 如果需要检查购买状态，则远程调用"用户服务"获取声音购买状态得到Map<Long, Integer>
            if (isNeedCheckPayStatus) {
                //4.3.1 找出本页中非试听声音ID列表作为检查购买状态声音ID列表
                List<Long> needChekPayStatusTrackIdList = pageInfo.getRecords().stream()
                        //排除掉试听
                        .filter(track -> track.getOrderNum() > tracksForFree)
                        //获取声音ID
                        .map(AlbumTrackListVo::getTrackId)
                        //收集声音ID
                        .collect(Collectors.toList());
                //4.3.2 远程调用"用户服务"获取声音购买状态
                Map<Long, Integer> payStatusMap = userFeignClient.userIsPaidTrack(userId, albumId, needChekPayStatusTrackIdList).getData();
                //4.4 处理当前页中声音购买状态标识，如果未购买将购买标识设置为True，反之采用默认值false
                pageInfo.getRecords().stream()
                        //试听声音记录不需要判断，付费标识保留默认false
                        .filter(track -> track.getOrderNum() > tracksForFree)
                        .forEach(track -> track.setIsShowPaidMark(payStatusMap.get(track.getTrackId()).intValue() == 0));
                //.forEach(track -> {
                //    Integer payStatus = payStatusMap.get(track.getTrackId());
                //    if (payStatus.intValue() == 0) {
                //        //未购买声音，将付费标识设置为：True
                //        track.setIsShowPaidMark(true);
                //    }
                //});
            }
        }
        return pageInfo;
    }

    /**
     * 增量更新声音统计数值
     *
     * @param trackStatMqVo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrackStat(TrackStatMqVo trackStatMqVo) {
        //1.更新声音统计表
        trackStatMapper.update(
                null,
                new LambdaUpdateWrapper<TrackStat>()
                        .eq(TrackStat::getTrackId, trackStatMqVo.getTrackId())
                        .eq(TrackStat::getStatType, trackStatMqVo.getStatType())
                        .setSql("stat_num = stat_num + " + trackStatMqVo.getCount())
        );
        //2.如果统计类型是：播放量、评论量 所属专辑统计信息需更新
        if (TRACK_STAT_PLAY.equals(trackStatMqVo.getStatType())) {
            //2.1. 更新专辑播放量
            albumStatMapper.update(
                    null,
                    new LambdaUpdateWrapper<AlbumStat>()
                            .eq(AlbumStat::getAlbumId, trackStatMqVo.getAlbumId())
                            .eq(AlbumStat::getStatType, ALBUM_STAT_PLAY)
                            .setSql("stat_num = stat_num + " + trackStatMqVo.getCount())
            );
        }
        if (TRACK_STAT_COMMENT.equals(trackStatMqVo.getStatType())) {
            //2.2. 更新专辑评论量
            albumStatMapper.update(
                    null,
                    new LambdaUpdateWrapper<AlbumStat>()
                            .eq(AlbumStat::getAlbumId, trackStatMqVo.getAlbumId())
                            .eq(AlbumStat::getStatType, ALBUM_STAT_COMMENT)
                            .setSql("stat_num = stat_num + " + trackStatMqVo.getCount())
            );
        }
        if (trackStatMqVo.getAlbumId() != null) {
            cacheInvalidationService.evictAfterCommit("albuminfo:stat:" + trackStatMqVo.getAlbumId());
            cacheInvalidationService.evictAfterCommit(RedisConstant.ALBUM_DETAIL_PREFIX + trackStatMqVo.getAlbumId());
        }
    }

    /**
     * 查询声音统计信息
     *
     * @param trackId
     * @return
     */
    @Override
    public TrackStatVo getTrackStatVo(Long trackId) {
        return trackInfoMapper.getTrackStatVo(trackId);
    }

    /**
     * 基于用户选择购买声音作为标准找出未购买声音数量动态构建分集购买列表
     *
     * @param userId  用户ID
     * @param trackId 声音ID 选中购买声音ID
     * @return [{name:"本集",price:0.2,trackCount:1},{name:"后10集",price:2,trackCount:10},{name:"全集",price:6.2,trackCount:31},]
     */
    @Override
    public List<Map<String, Object>> findUserTrackPaidList(Long trackId, Long userId) {
        //1.查询选中作为标准声音对象 得到序号、专辑ID
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        Integer orderNum = trackInfo.getOrderNum();
        Long albumId = trackInfo.getAlbumId();
        //2.根据序号+专辑ID查询，选中专辑到最后声音列表（可能包含用户已购买声音）
        List<TrackInfo> trackInfoList = trackInfoMapper.selectList(
                new LambdaQueryWrapper<TrackInfo>()
                        .eq(TrackInfo::getAlbumId, albumId)
                        .ge(TrackInfo::getOrderNum, orderNum)
                        .select(TrackInfo::getId)
        );

        //3.远程调用"用户服务"查询用户已购买声音
        List<Long> userPaidTrackIdList = userFeignClient.findUserPaidTrackList(albumId).getData();
        if (CollUtil.isNotEmpty(userPaidTrackIdList)) {
            //4.通过stream过滤掉已购买声音
            trackInfoList = trackInfoList.stream()
                    .filter(t -> !userPaidTrackIdList.contains(t.getId()))
                    .collect(Collectors.toList());

        }


        //5.基于未购买声音数量动态构建分集购买列表
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        //5.1 查询专辑得到单集声音价格
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        BigDecimal price = albumInfo.getPrice();
        int size = trackInfoList.size();
        //5.1 构建"本集"分集购买对象
        list.add(Map.of("name", "本集", "price", price, "trackCount", 1));
        //5.2 构建"后N集"分集购买对象 例如：size=34 展示：本集、后10集、后20集、后30集、全集
        for (int i = 10; i <= 50; i += 10) {
            if (i < size) {
                list.add(Map.of("name", "后" + i + "集", "price", price.multiply(new BigDecimal(i)), "trackCount", i));
            } else {
                list.add(Map.of("name", "全集", "price", price.multiply(new BigDecimal(size)), "trackCount", size));
                break;
            }
        }
        return list;
    }

    /**
     * 以提交声音ID作为标准，查询未购买声音列表
     *
     * @param trackId    提交声音ID
     * @param trackCount 声音数量
     * @return 声音列表
     */
    @Override
    public List<TrackInfo> findPaidTrackInfoList(Long trackId, Integer trackCount, Long userId) {
        //1.查询提交声音对象
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        Long albumId = trackInfo.getAlbumId();
        Integer orderNum = trackInfo.getOrderNum();
        //2.构建查询条件
        //2.1 专辑ID，序号
        LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrackInfo::getAlbumId, albumId);
        queryWrapper.ge(TrackInfo::getOrderNum, orderNum);
        //多查询字段专辑ID： 在订单服务根据声音获取到专辑ID 进一步获取声音单价
        queryWrapper.select(TrackInfo::getId, TrackInfo::getTrackTitle, TrackInfo::getCoverUrl, TrackInfo::getAlbumId);
        queryWrapper.last("limit " + trackCount);

        //2.2 远程调用用户服务获取已购买声音ID
        List<Long> userPaidTrackIdList = userFeignClient.findUserPaidTrackList(albumId).getData();
        if (CollUtil.isNotEmpty(userPaidTrackIdList)) {
            queryWrapper.notIn(TrackInfo::getId, userPaidTrackIdList);
        }

        //2.执行查询
        return trackInfoMapper.selectList(queryWrapper);
    }
}
