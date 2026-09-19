package com.luojia.soundscape.order.pattern;

import com.luojia.soundscape.vo.order.OrderInfoVo;
import com.luojia.soundscape.vo.order.TradeVo;

/**
 * 结算不同商品类型 策略接口
 * @author Luojia Soundscape Contributors
 */
public interface TradeStrategy {

    /**
     * 对不同商品类型结算抽象方法
     * @param tradeVo
     * @param userId
     * @return
     */
    OrderInfoVo trade(TradeVo tradeVo, Long userId);

}
