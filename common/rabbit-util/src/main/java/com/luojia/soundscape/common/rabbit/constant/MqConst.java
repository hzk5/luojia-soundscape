package com.luojia.soundscape.common.rabbit.constant;

public class MqConst {

    public static final String EXCHANGE_CACHE_INVALIDATE = "luojia-soundscape.cache.invalidate";

    /**
     * 专辑
     */
    public static final String EXCHANGE_ALBUM = "luojia-soundscape.album";
    public static final String ROUTING_ALBUM_UPPER  = "luojia-soundscape.album.upper";
    public static final String ROUTING_ALBUM_LOWER  = "luojia-soundscape.album.lower";
    public static final String ROUTING_ALBUM_STAT_UPDATE = "luojia-soundscape.album.stat.update";
    public static final String ROUTING_ALBUM_ES_STAT_UPDATE = "luojia-soundscape.album.es.stat.update";
    public static final String ROUTING_ALBUM_RANKING_UPDATE = "luojia-soundscape.album.ranking.update";
    //队列
    public static final String QUEUE_ALBUM_UPPER  = "luojia-soundscape.album.upper";
    public static final String QUEUE_ALBUM_LOWER  = "luojia-soundscape.album.lower";
    public static final String QUEUE_ALBUM_STAT_UPDATE = "luojia-soundscape.album.stat.update";
    public static final String QUEUE_ALBUM_ES_STAT_UPDATE = "luojia-soundscape.album.es.stat.update";
    public static final String QUEUE_ALBUM_RANKING_UPDATE = "luojia-soundscape.album.ranking.update";

    /**
     * 声音
     */
    public static final String EXCHANGE_TRACK = "luojia-soundscape.track";
    public static final String ROUTING_TRACK_STAT_UPDATE = "luojia-soundscape.track.stat.update";
    public static final String QUEUE_TRACK_STAT_UPDATE = "luojia-soundscape.track.stat.update";

    /**
     * 取消订单
     */
    //延迟取消订单队列
    /**
     * 取消订单延迟消息
     */
    public static final String EXCHANGE_CANCEL_ORDER = "luojia-soundscape.cancel.order";
    public static final String ROUTING_CANCEL_ORDER = "luojia-soundscape.cancel.order";
    public static final String QUEUE_CANCEL_ORDER = "luojia-soundscape.cancel.order";
    public static final Integer CANCEL_ORDER_DELAY_TIME = 15 * 60;

    /**
     * 支付
     */
    public static final String EXCHANGE_ORDER = "luojia-soundscape.order";
    public static final String ROUTING_ORDER_PAY_SUCCESS  = "luojia-soundscape.order.pay.success";
    public static final String ROUTING_RECHARGE_PAY_SUCCESS  = "luojia-soundscape.recharge.pay.success";
    public static final String QUEUE_ORDER_PAY_SUCCESS  = "luojia-soundscape.order.pay.success";
    public static final String QUEUE_RECHARGE_PAY_SUCCESS  = "luojia-soundscape.recharge.pay.success";


    /**
     * 账户
     */
    public static final String EXCHANGE_ACCOUNT = "luojia-soundscape.account";
    public static final String ROUTING_ACCOUNT_UNLOCK  = "luojia-soundscape.account.unlock";
    public static final String ROUTING_ACCOUNT_MINUS  = "luojia-soundscape.account.minus";
    public static final String QUEUE_ACCOUNT_UNLOCK  = "luojia-soundscape.account.unlock";
    public static final String QUEUE_ACCOUNT_MINUS  = "luojia-soundscape.account.minus";

    /**
     * 用户
     */
    public static final String EXCHANGE_USER = "luojia-soundscape.user";
    public static final String ROUTING_USER_PAY_RECORD  = "luojia-soundscape.user.pay.record";
    public static final String ROUTING_USER_REGISTER  = "luojia-soundscape.user.register";
    public static final String ROUTING_USER_VIP_EXPIRE_STATUS = "luojia-soundscape.user.vip.expire.status";
    public static final String QUEUE_USER_PAY_RECORD  = "luojia-soundscape.user.pay.record";
    public static final String QUEUE_USER_REGISTER  = "luojia-soundscape.user.register";
    public static final String QUEUE_USER_VIP_EXPIRE_STATUS = "luojia-soundscape.user.vip.expire.status";

    /**
     * 热门关键字
     */
    public static final String EXCHANGE_KEYWORD = "luojia-soundscape.keyword";
    public static final String ROUTING_KEYWORD_INPUT  = "luojia-soundscape.keyword.input";
    public static final String ROUTING_KEYWORD_OUT  = "luojia-soundscape.keyword.out";
    public static final String QUEUE_KEYWORD_INPUT  = "luojia-soundscape.keyword.input";
    public static final String QUEUE_KEYWORD_OUT  = "luojia-soundscape.keyword.out";


}
