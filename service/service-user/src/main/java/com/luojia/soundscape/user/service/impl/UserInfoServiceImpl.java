package com.luojia.soundscape.user.service.impl;

import java.util.Date;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.luojia.soundscape.album.AlbumFeignClient;
import com.luojia.soundscape.model.album.TrackInfo;
import com.luojia.soundscape.query.album.BatchIdQuery;
import com.luojia.soundscape.model.user.*;
import com.luojia.soundscape.user.mapper.*;
import com.luojia.soundscape.user.pattern.DeliveryStrategy;
import com.luojia.soundscape.user.pattern.factory.DeliveryStrategyFactory;
import com.luojia.soundscape.user.pattern.impl.TrackDelivery;
import com.luojia.soundscape.user.pattern.impl.VIPDelivery;
import com.google.common.collect.Maps;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.luojia.soundscape.common.cache.SoundscapeCache;
import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.common.rabbit.constant.MqConst;
import com.luojia.soundscape.common.rabbit.service.RabbitService;
import com.luojia.soundscape.common.rabbit.service.CacheInvalidationService;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.common.util.MongoUtil;
import com.luojia.soundscape.user.service.UserInfoService;
import com.luojia.soundscape.vo.user.UserCollectVo;
import com.luojia.soundscape.vo.user.UserInfoVo;
import com.luojia.soundscape.vo.user.UserPaidRecordVo;
import com.luojia.soundscape.vo.user.UserSubscribeVo;
import com.luojia.soundscape.vo.album.AlbumBriefVo;
import com.luojia.soundscape.vo.album.TrackBriefVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private CacheInvalidationService cacheInvalidationService;

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;

    /**
     * 微信一键登录
     *
     * @param code 小程序端对接微信获取临时登录凭证code 5分钟只能使用一次
     * @return {token:"访问令牌"}
     */
    @Override
    public Map<String, String> wxLogin(String code) {
        try {
            //1. 对接微信获取微信账户唯一标识
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            Assert.notNull(sessionInfo, "登录,code:{}失败", code);
            String wxOpenId = sessionInfo.getOpenid();

            //2. 根据唯一标识查询本地用户信息
            UserInfo userInfo = userInfoMapper.selectOne(
                    new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getWxOpenId, wxOpenId)
            );

            //3. 如果用户信息为空
            if (userInfo == null) {
                //3.1 新增用户信息（关联微信唯一标识）
                userInfo = new UserInfo();
                userInfo.setNickname("听友" + IdUtil.nanoId());
                userInfo.setAvatarUrl("/static/default-avatar.png");
                userInfo.setWxOpenId(wxOpenId);
                userInfoMapper.insert(userInfo);
                //3.2 TODO 基于RabbitMQ隐式初始化账号（余额）信息
                //3.2.1 准备初始化账户对象采用Map
                Map<String, Object> map = new HashMap<>();
                map.put("userId", userInfo.getId());
                map.put("amount", new BigDecimal("1000.00"));
                map.put("orderNo", "ZS" + IdUtil.getSnowflakeNextIdStr());
                map.put("title", "新用户注册赠送");
                //3.2.2 调用Rabbit生产者工具类发送消息  对象必须实现序列化接口
                rabbitService.sendMessage(MqConst.EXCHANGE_USER, MqConst.ROUTING_USER_REGISTER, map);
            }

            //4. 基于用户信息生成令牌,将令牌、用户基本信息 存入Redis
            //4.1 构建Redis登录信息key 采用UUID做为token
            String token = IdUtil.fastUUID();
            String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
            //4.2 将用户信息转为VO
            UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
            //4.3 存入Redis
            redisTemplate.opsForValue().set(loginKey, userInfoVo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
            //5. 响应登录成功令牌
            return Map.of("token", token);
        } catch (WxErrorException e) {
            log.error("微信登录失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @SoundscapeCache(prefix = "user:userinfovo:")
    public UserInfoVo getUserInfoVo(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        return BeanUtil.copyProperties(userInfo, UserInfoVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UserInfoVo userInfoVo) {
        //只允许修改基本信息 昵称、头像
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
        userInfo.setNickname(userInfoVo.getNickname());
        userInfoMapper.updateById(userInfo);

        // 用户资料已更新，删除旧的用户信息缓存，避免前端继续读取旧头像和昵称。
        cacheInvalidationService.evictAfterCommit("user:userinfovo:" + userId);
    }

    @Override
    public Boolean subscribe(Long userId, Long albumId) {
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_SUBSCRIBE, userId);
        Query query = new Query(Criteria.where("userId").is(userId).and("albumId").is(albumId));
        UserSubscribe existing = mongoTemplate.findOne(query, UserSubscribe.class, collectionName);
        if (existing != null) {
            mongoTemplate.remove(query, UserSubscribe.class, collectionName);
            return false;
        }

        UserSubscribe subscribe = new UserSubscribe();
        subscribe.setUserId(userId);
        subscribe.setAlbumId(albumId);
        subscribe.setCreateTime(new Date());
        mongoTemplate.save(subscribe, collectionName);
        return true;
    }

    @Override
    public Boolean isSubscribe(Long userId, Long albumId) {
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_SUBSCRIBE, userId);
        Query query = new Query(Criteria.where("userId").is(userId).and("albumId").is(albumId));
        return mongoTemplate.exists(query, UserSubscribe.class, collectionName);
    }

    @Override
    public IPage<UserSubscribeVo> findUserSubscribePage(Long userId, long page, long limit) {
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_SUBSCRIBE, userId);
        Query countQuery = new Query(Criteria.where("userId").is(userId));
        long total = mongoTemplate.count(countQuery, UserSubscribe.class, collectionName);
        Query pageQuery = new Query(Criteria.where("userId").is(userId));
        pageQuery.with(PageRequest.of(Math.max((int) page - 1, 0), (int) limit,
                Sort.Direction.DESC, "createTime"));

        List<UserSubscribe> subscriptions = mongoTemplate.find(pageQuery, UserSubscribe.class, collectionName);
        BatchIdQuery batchQuery = new BatchIdQuery();
        batchQuery.setIds(subscriptions.stream().map(UserSubscribe::getAlbumId).distinct().toList());
        Result<List<AlbumBriefVo>> batchResult = batchQuery.getIds().isEmpty()
                ? Result.ok(List.of()) : albumFeignClient.getAlbumInfoBatch(batchQuery);
        Map<Long, AlbumBriefVo> albumMap = batchResult != null && batchResult.getData() != null
                ? batchResult.getData().stream().collect(Collectors.toMap(AlbumBriefVo::getId, item -> item, (left, right) -> left))
                : Map.of();

        List<UserSubscribeVo> records = subscriptions.stream().map(item -> {
                    UserSubscribeVo vo = new UserSubscribeVo();
                    vo.setAlbumId(item.getAlbumId());
                    vo.setCreateTime(item.getCreateTime());
                    AlbumBriefVo album = albumMap.get(item.getAlbumId());
                    if (album != null) {
                        vo.setAlbumTitle(album.getAlbumTitle());
                        vo.setCoverUrl(album.getCoverUrl());
                        vo.setIncludeTrackCount(album.getIncludeTrackCount());
                        vo.setIsFinished(album.getIsFinished());
                    }
                    return vo;
                }).collect(Collectors.toList());

        return new Page<UserSubscribeVo>(page, limit, total).setRecords(records);
    }

    @Override
    public Boolean collect(Long userId, Long trackId) {
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId);
        Query query = new Query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        UserCollect existing = mongoTemplate.findOne(query, UserCollect.class, collectionName);
        if (existing != null) {
            mongoTemplate.remove(query, UserCollect.class, collectionName);
            return false;
        }

        UserCollect collect = new UserCollect();
        collect.setUserId(userId);
        collect.setTrackId(trackId);
        collect.setCreateTime(new Date());
        mongoTemplate.save(collect, collectionName);
        return true;
    }

    @Override
    public Boolean isCollect(Long userId, Long trackId) {
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId);
        Query query = new Query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        return mongoTemplate.exists(query, UserCollect.class, collectionName);
    }

    @Override
    public IPage<UserCollectVo> findUserCollectPage(Long userId, long page, long limit) {
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId);
        Query countQuery = new Query(Criteria.where("userId").is(userId));
        long total = mongoTemplate.count(countQuery, UserCollect.class, collectionName);
        Query pageQuery = new Query(Criteria.where("userId").is(userId));
        pageQuery.with(PageRequest.of(Math.max((int) page - 1, 0), (int) limit,
                Sort.Direction.DESC, "createTime"));

        List<UserCollect> collections = mongoTemplate.find(pageQuery, UserCollect.class, collectionName);
        BatchIdQuery batchQuery = new BatchIdQuery();
        batchQuery.setIds(collections.stream().map(UserCollect::getTrackId).distinct().toList());
        Result<List<TrackBriefVo>> batchResult = batchQuery.getIds().isEmpty()
                ? Result.ok(List.of()) : albumFeignClient.getTrackInfoBatch(batchQuery);
        Map<Long, TrackBriefVo> trackMap = batchResult != null && batchResult.getData() != null
                ? batchResult.getData().stream().collect(Collectors.toMap(TrackBriefVo::getId, item -> item, (left, right) -> left))
                : Map.of();

        List<UserCollectVo> records = collections.stream().map(item -> {
                    UserCollectVo vo = new UserCollectVo();
                    vo.setTrackId(item.getTrackId());
                    vo.setCreateTime(item.getCreateTime());
                    TrackBriefVo track = trackMap.get(item.getTrackId());
                    if (track != null) {
                        vo.setAlbumId(track.getAlbumId());
                        vo.setTrackTitle(track.getTrackTitle());
                        vo.setCoverUrl(track.getCoverUrl());
                    }
                    return vo;
                }).collect(Collectors.toList());

        return new Page<UserCollectVo>(page, limit, total).setRecords(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateVipExpireStatus(Date date) {
        List<UserInfo> expiredUsers = this.list(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getIsVip, 1)
                        .isNotNull(UserInfo::getVipExpireTime)
                        .le(UserInfo::getVipExpireTime, date)
                        .select(UserInfo::getId)
        );
        if (CollUtil.isEmpty(expiredUsers)) {
            return 0;
        }

        List<Long> userIds = expiredUsers.stream()
                .map(UserInfo::getId)
                .collect(Collectors.toList());
        int updated = userInfoMapper.update(
                null,
                new LambdaUpdateWrapper<UserInfo>()
                        .in(UserInfo::getId, userIds)
                        .eq(UserInfo::getIsVip, 1)
                        .le(UserInfo::getVipExpireTime, date)
                        .set(UserInfo::getIsVip, 0)
        );
        if (updated > 0) {
            List<String> cacheKeys = userIds.stream()
                    .map(id -> "user:userinfovo:" + id)
                    .collect(Collectors.toList());
            cacheKeys.forEach(cacheInvalidationService::evictAfterCommit);
        }
        return updated;
    }


    /**
     * 检查每个提交声音购买状态，如果已购买将购买状态设置为1，反之设置为0
     *
     * @param userId                        用户ID
     * @param albumId                       专辑ID
     * @param needCheckPayStatusTrackIdList 待检查购买状态声音ID列表
     * @return 每个声音购买状态 {声音ID:购买状态}
     */
    @Override
    public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStatusTrackIdList) {
        //1.根据用户ID+专辑ID 查询已购专辑表
        Long count = userPaidAlbumMapper.selectCount(
                new LambdaQueryWrapper<UserPaidAlbum>()
                        .eq(UserPaidAlbum::getUserId, userId)
                        .eq(UserPaidAlbum::getAlbumId, albumId)
        );

        //如果购买专辑，则将所有声音购买状态设置1，返回即可
        HashMap<Long, Integer> map = new HashMap<>();
        if (count > 0) {
            for (Long trackId : needCheckPayStatusTrackIdList) {
                map.put(trackId, 1);
            }
            return map;
        }

        //2.根据用户ID+专辑ID 查询已购声音表
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(
                new LambdaQueryWrapper<UserPaidTrack>()
                        .eq(UserPaidTrack::getUserId, userId)
                        .eq(UserPaidTrack::getAlbumId, albumId)
                        .select(UserPaidTrack::getTrackId)
        );
        //2.1 如果不存在声音购买记录，则将所有声音购买状态设置0，返回即可。
        if (CollUtil.isEmpty(userPaidTrackList)) {
            //说明当前用户未购买专辑且未购买任何声音
            for (Long trackId : needCheckPayStatusTrackIdList) {
                map.put(trackId, 0);
            }
            return map;
        }
        //2.2 如果存在声音购买记录，找出已购买（购买状态设置为1）以及未购买（购买状态设置为0）设置相应购买状态
        List<Long> userPaidTrackIdList = userPaidTrackList.stream()
                .map(UserPaidTrack::getTrackId).collect(Collectors.toList());
        //2.3 循环待检查购买状态声音ID列表
        for (Long trackId : needCheckPayStatusTrackIdList) {
            if (userPaidTrackIdList.contains(trackId)) {
                //包含在已购声音ID中
                map.put(trackId, 1);
            } else {
                map.put(trackId, 0);
            }
        }
        return map;
    }

    /**
     * 检查用户是否已购买专辑
     *
     * @param albumId
     * @return true:已购买 false:未购买
     */
    @Override
    public Boolean isPaidAlbum(Long userId, Long albumId) {
        Long count = userPaidAlbumMapper.selectCount(
                new LambdaQueryWrapper<UserPaidAlbum>()
                        .eq(UserPaidAlbum::getUserId, userId)
                        .eq(UserPaidAlbum::getAlbumId, albumId)
        );
        return count > 0;
    }

    /**
     * 查询用户已购买的声音ID列表
     *
     * @param albumId 专辑ID
     * @return 已购声音ID列表
     */
    @Override
    public List<Long> findUserPaidTrackList(Long userId, Long albumId) {
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(
                new LambdaQueryWrapper<UserPaidTrack>()
                        .eq(UserPaidTrack::getUserId, userId)
                        .eq(UserPaidTrack::getAlbumId, albumId)
                        .select(UserPaidTrack::getTrackId)
        );
        if (CollUtil.isNotEmpty(userPaidTrackList)) {
            List<Long> paidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
            return paidTrackIdList;
        }
        return List.of();
    }

    @Autowired
    private DeliveryStrategyFactory deliveryStrategyFactory;

    /**
     * 用户付款（余额、微信）成功后，发放权益（VIP、专辑、声音）
     *
     * @param userPaidRecordVo
     * @return
     */
    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //获取付款项目类型: 1001-专辑 1002-声音 1003-vip会员
        DeliveryStrategy strategy = deliveryStrategyFactory.getStrategy(userPaidRecordVo.getItemType());
        strategy.delivery(userPaidRecordVo);
        /*//1.处理付款项目类型为专辑，权益发放
        if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)) {

            //1.1 判断该笔订单是否已处理
            Long count = userPaidAlbumMapper.selectCount(
                    new LambdaQueryWrapper<UserPaidAlbum>()
                            .eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo())
            );
            if (count == 0) {
                //1.2 构建专辑购买记录
                UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
                userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
                userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
                userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
                //3.3 保存专辑购买记录
                userPaidAlbumMapper.insert(userPaidAlbum);
            }
        } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)) {
            //2.处理付款项目类型为声音，权益发放
            //2.1 判断该笔订单是否已处理
            Long count = userPaidTrackMapper.selectCount(
                    new LambdaQueryWrapper<UserPaidTrack>()
                            .eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo())
            );
            //2.2 构建声音多条购买记录
            if (count == 0) {
                //根据声音ID查询声音对象获取所属专辑ID
                TrackInfo trackInfo = albumFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0)).getData();
                Long albumId = trackInfo.getAlbumId();
                for (Long trackId : userPaidRecordVo.getItemIdList()) {
                    UserPaidTrack userPaidTrack = new UserPaidTrack();
                    userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
                    userPaidTrack.setUserId(userPaidRecordVo.getUserId());
                    userPaidTrack.setAlbumId(albumId);
                    userPaidTrack.setTrackId(trackId);
                    //2.3 保存声音购买记录
                    userPaidTrackMapper.insert(userPaidTrack);
                }
            }
        } else if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)) {
            //3.处理付款项目类型为VIP会员，权益发放
            Date now = new Date();
            //3.1 判断该笔订单是否已处理
            Long count = userVipServiceMapper.selectCount(
                    new LambdaQueryWrapper<UserVipService>()
                            .eq(UserVipService::getOrderNo, userPaidRecordVo.getOrderNo())
            );
            if (count == 0) {
                //3.2 查询用户得到目前身份
                Boolean isVIP = false;
                UserInfoVo userInfoVo = this.getUserInfoVo(userPaidRecordVo.getUserId());
                if (userInfoVo.getIsVip().intValue() == 1 && userInfoVo.getVipExpireTime().after(now)) {
                    isVIP = true;
                }
                //3.3 创建会员购买记录 封装：生效时间、结束时间。保存购买记录
                UserVipService userVipService = new UserVipService();
                userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
                userVipService.setUserId(userPaidRecordVo.getUserId());
                //3.3.0 获取选购套餐信息
                VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(userPaidRecordVo.getItemIdList().get(0));
                Integer serviceMonth = vipServiceConfig.getServiceMonth();
                //3.3.1 计算本次会员生效时间
                if (!isVIP) {
                    //当前是普通用户 起始时间=当前时间 过期时间=当前时间+会员时长
                    userVipService.setStartTime(now);
                    //3.3.2 计算本次会员到期时间
                    DateTime endTime = DateUtil.offsetMonth(now, serviceMonth);
                    userVipService.setExpireTime(endTime);
                }else{
                    //当前已是VIP用户 起始时间=当前会员到期时间+1天
                    DateTime startTime = DateUtil.offsetDay(userInfoVo.getVipExpireTime(), 1);
                    userVipService.setStartTime(startTime);
                    DateTime endTime = DateUtil.offsetMonth(startTime, serviceMonth);
                    userVipService.setExpireTime(endTime);
                }
                /// 3.4 保存会员购买记录
                userVipServiceMapper.insert(userVipService);
                //3.4 更新用户会员标识、过期时间
                UserInfo userInfo = new UserInfo();
                userInfo.setId(userPaidRecordVo.getUserId());
                userInfo.setIsVip(1);
                userInfo.setVipExpireTime(userVipService.getExpireTime());
                userInfoMapper.updateById(userInfo);
            }
        }*/
    }
}
