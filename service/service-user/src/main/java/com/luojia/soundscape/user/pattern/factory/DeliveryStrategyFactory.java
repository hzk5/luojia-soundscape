package com.luojia.soundscape.user.pattern.factory;

import com.luojia.soundscape.user.pattern.DeliveryStrategy;
import com.luojia.soundscape.user.pattern.impl.TrackDelivery;
import com.luojia.soundscape.user.pattern.impl.VIPDelivery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 工厂：统一自动发现所有策略实现类对象、根据传入购买项目类型返回对应的策略实现类对象
 *
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@Component
public class DeliveryStrategyFactory {


    /**
     * 将Map中Value类型下所有实现类对象存储在Map中
     * Map中key是策略实现类对象BeanID value是策略实现类对象
     */
    @Autowired
    private Map<String, DeliveryStrategy> strategyMap;

    /**
     * 将DeliveryStrategy下所有实现类对象注入List中
     */
    @Autowired
    private List<DeliveryStrategy> strategyList;


    /**
     * 根据传入的购买项目类型返回对应的策略实现类对象
     * @param itemType 项目类型 1001-专辑 1002-声音 1003-vip会员
     * @return
     */
    public DeliveryStrategy getStrategy(String itemType) {
        if (strategyMap.containsKey(itemType)) {
            return strategyMap.get(itemType);
        }
        throw new RuntimeException("没有对应的策略实现类对象");
    }
}
