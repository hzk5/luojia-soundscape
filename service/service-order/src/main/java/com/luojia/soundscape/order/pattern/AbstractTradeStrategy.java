package com.luojia.soundscape.order.pattern;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.order.helper.SignHelper;
import com.luojia.soundscape.vo.order.OrderInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 抽象策略父类
 * @author Luojia Soundscape Contributors
 */
public abstract class AbstractTradeStrategy implements TradeStrategy{

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 生成本次订单流水号
     * @param orderInfoVo
     * @param userId
     */
    public void generateTradeNo(OrderInfoVo orderInfoVo, Long userId){
        //5.3.2 流水号机制，防止订单重复提交（1.用户网络卡连续点击结算按钮2.成功提交订单，回退到订单确认页再次提交）
        //5.3.2.1 构建当前用户当前订单流水号Key
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        //5.3.2.2 采用UUID生成本次订单流水号值
        String tradeNo = IdUtil.randomUUID();
        //5.3.2.3 将流水号值保存到Redis中，设置过期时间（5分钟），防止订单重复提交
        redisTemplate.opsForValue().set(tradeKey, tradeNo, RedisConstant.ORDER_TRADE_EXPIRE, TimeUnit.MINUTES);
        //5.3.2.4 为订单VO封装流水号值
        orderInfoVo.setTradeNo(tradeNo);
    }


    /**
     * 生成本次订单签名
     * @param orderInfoVo
     * @param userId
     */
    public void generateSign(OrderInfoVo orderInfoVo, Long userId){
        //5.3.3 签名机制，时间戳+签名值，防止订单结算页数据被篡改
        //5.3.1 将订单VO转为Map对象，由于无法确定支付方式，固将VO中payWay为空属性排除掉
        orderInfoVo.setTimestamp(System.currentTimeMillis());
        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo, false, true);
        //5.3.2 调用生成签名工具方法，底层就是Md5,对订单VO中所有属性值进行排序，拼接成字符串+秘钥，进行MD5加密，返回签名值
        String sign = SignHelper.getSign(map);
        orderInfoVo.setSign(sign);
    }


}
