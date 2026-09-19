package com.luojia.soundscape.order.pattern.factory;

import com.luojia.soundscape.order.pattern.TradeStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Luojia Soundscape Contributors
 */
@Component
public class TradeStrategyFactory {

    /**
     * 从IOC容器中获取TradeStrategy接口下所有实现类对象注入到Map中
     * Map中Key：策略实现类对象BeanID
     * Map中Value：策略实现类对象
     */
    @Autowired
    private Map<String, TradeStrategy> tradeStrategyMap;


    /**
     * 根据购买项目类型itemType获取对应的策略实现类对象
     * @param itemType
     * @return
     */
    public TradeStrategy getTradeStrategy(String itemType) {
        return tradeStrategyMap.get(itemType);
    }
}
