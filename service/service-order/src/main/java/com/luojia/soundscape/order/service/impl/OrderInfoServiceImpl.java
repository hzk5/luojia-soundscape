package com.luojia.soundscape.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.luojia.soundscape.account.AccountFeignClient;
import com.luojia.soundscape.album.AlbumFeignClient;
import com.luojia.soundscape.common.constant.RedisConstant;
import com.luojia.soundscape.common.constant.SystemConstant;
import com.luojia.soundscape.common.execption.SoundscapeException;
import com.luojia.soundscape.common.rabbit.constant.MqConst;
import com.luojia.soundscape.common.rabbit.service.RabbitService;
import com.luojia.soundscape.common.result.Result;
import com.luojia.soundscape.common.util.AuthContextHolder;
import com.luojia.soundscape.model.album.AlbumInfo;
import com.luojia.soundscape.model.album.TrackInfo;
import com.luojia.soundscape.model.order.OrderDerate;
import com.luojia.soundscape.model.order.OrderDetail;
import com.luojia.soundscape.model.order.OrderInfo;
import com.luojia.soundscape.model.user.VipServiceConfig;
import com.luojia.soundscape.order.helper.SignHelper;
import com.luojia.soundscape.order.mapper.OrderDerateMapper;
import com.luojia.soundscape.order.mapper.OrderInfoMapper;
import com.luojia.soundscape.order.pattern.TradeStrategy;
import com.luojia.soundscape.order.pattern.factory.TradeStrategyFactory;
import com.luojia.soundscape.order.service.OrderDetailService;
import com.luojia.soundscape.order.service.OrderInfoService;
import com.luojia.soundscape.user.client.UserFeignClient;
import com.luojia.soundscape.vo.account.AccountDeductVo;
import com.luojia.soundscape.vo.order.OrderDerateVo;
import com.luojia.soundscape.vo.order.OrderDetailVo;
import com.luojia.soundscape.vo.order.OrderInfoVo;
import com.luojia.soundscape.vo.order.TradeVo;
import com.luojia.soundscape.vo.user.UserInfoVo;
import com.luojia.soundscape.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.luojia.soundscape.common.constant.SystemConstant.*;

@Slf4j
@Service
@RefreshScope  // Value注解 刷新配置文件
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;


    @Value("${order.cancel}")
    private Integer cancelTTL;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private OrderDerateMapper orderDerateMapper;

    @Autowired
    private AccountFeignClient accountFeignClient;

    @Autowired
    private TradeStrategyFactory tradeStrategyFactory;

    /**
     * 订单结算（对三种不同商品类型进行结算，渲染结算页）
     *
     * @param tradeVo 包含：购买项目类型、购买项目ID、声音数量
     * @return 订单VO
     */
    @Override
    public OrderInfoVo trade(TradeVo tradeVo) {
        //1.根据付款项目类型获取对应的策略实现类对象
        TradeStrategy tradeStrategy = tradeStrategyFactory.getTradeStrategy(tradeVo.getItemType());
        //2.调用不同策略实现类对象进行订单结算
        return tradeStrategy.trade(tradeVo, AuthContextHolder.getUserId());
        /*//1.初始化OrderInfoVo对象
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        //2.初始化相关金额变量、订单明细列表、订单减免明细列表
        BigDecimal originalAmount = new BigDecimal("0.00");
        BigDecimal derateAmount = new BigDecimal("0.00");
        BigDecimal orderAmount = new BigDecimal("0.00");
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();

        //获取购买项目类型：1001-专辑 1002-声音 1003-vip会员
        String itemType = tradeVo.getItemType();
        Long userId = AuthContextHolder.getUserId();
        //3.处理商品类型为：VIP套餐
        if (ORDER_ITEM_TYPE_VIP.equals(itemType)) {
            //3.1 远程调用"用户服务"获取购买套餐信息
            VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(tradeVo.getItemId()).getData();
            Assert.notNull(vipServiceConfig, "获取VIP套餐信息失败！");
            //3.2 封装价格：原价、减免金额、订单金额
            originalAmount = vipServiceConfig.getPrice();
            orderAmount = vipServiceConfig.getDiscountPrice();
            //如果原价大于订单价 存在优惠
            if (originalAmount.compareTo(orderAmount) == 1) {
                derateAmount = originalAmount.subtract(orderAmount);
            }
            //3.3 封装订单明细列表
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName("会员套餐：" + vipServiceConfig.getName());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVoList.add(orderDetailVo);

            //3.4 封装订单减免明细列表
            if (originalAmount.compareTo(orderAmount) == 1) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(ORDER_DERATE_VIP_SERVICE_DISCOUNT);
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setRemarks("限时会员减免");
                orderDerateVoList.add(orderDerateVo);
            }
        } else if (ORDER_ITEM_TYPE_ALBUM.equals(itemType)) {
            //4. 处理商品类型为：专辑
            //4.1 远程调用“用户”服务是否已购专辑,如果已购买 业务终止
            Long albumId = tradeVo.getItemId();
            Boolean flag = userFeignClient.isPaidAlbum(albumId).getData();
            if (flag) {
                throw new SoundscapeException(500, "请勿重复购买专辑");
            }
            //4.2 远程调用"专辑服务"得到专辑信息 获取：价格、普通用户折扣、会员折扣
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑{}不存在", albumId);
            //获取不同用户折扣
            BigDecimal discount = albumInfo.getDiscount();
            BigDecimal vipDiscount = albumInfo.getVipDiscount();
            originalAmount = albumInfo.getPrice();
            //暂时认为无折扣
            orderAmount = originalAmount;

            //4.3 远程调用用户服务获取用户身份，用与计算折扣

            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
            Assert.notNull(userInfoVo, "用户{}不存在", userId);
            Boolean isVIP = false;
            if (userInfoVo.getIsVip().intValue() == 1 && userInfoVo.getVipExpireTime().after(new Date())) {
                isVIP = true;
            }

            //4.4 封装价格：原价、减免金额、订单金额
            if (!isVIP && discount.doubleValue() != -1) {
                orderAmount = originalAmount.multiply(discount).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
            }
            if (isVIP && vipDiscount.doubleValue() != -1) {
                orderAmount = originalAmount.multiply(vipDiscount).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
            }
            if (originalAmount.compareTo(orderAmount) == 1) {
                derateAmount = originalAmount.subtract(orderAmount);
            }
            //4.5 封装订单明细列表
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(albumId);
            orderDetailVo.setItemName("专辑:" + albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVoList.add(orderDetailVo);

            //4.6 封装订单减免明细列表
            if (originalAmount.compareTo(orderAmount) == 1) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setRemarks("专辑限时减免");
                orderDerateVoList.add(orderDerateVo);
            }

        } else if (ORDER_ITEM_TYPE_TRACK.equals(itemType)) {
            //5. 处理商品类型为：声音
            //5.1 远程调用"专辑服务"获取用户未购买声音列表
            Long trackId = tradeVo.getItemId();
            List<TrackInfo> trackInfoList = albumFeignClient.findPaidTrackInfoList(trackId, tradeVo.getTrackCount()).getData();
            Assert.notNull(trackInfoList, "没有待结算声音");

            //5.2 远程调用"专辑服务"获取专辑中价格（声音单价）
            Long albumId = trackInfoList.get(0).getAlbumId();
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑{}不存在", albumId);
            BigDecimal price = albumInfo.getPrice();

            //5.3 封装价格：原价、订单金额 声音不支持折扣
            originalAmount = price.multiply(BigDecimal.valueOf(tradeVo.getTrackCount()));
            orderAmount = originalAmount;

            //5.4 封装订单明细列表
            orderDetailVoList = trackInfoList.stream().map(t -> {
                OrderDetailVo orderDetailVo = new OrderDetailVo();
                orderDetailVo.setItemId(t.getId());
                orderDetailVo.setItemName("声音：" + t.getTrackTitle());
                orderDetailVo.setItemUrl(t.getCoverUrl());
                orderDetailVo.setItemPrice(price);
                return orderDetailVo;
            }).collect(Collectors.toList());

        }

        //6.封装订单VO对象中属性
        //6.1 封装3个相关价格 ：原价、减免金额、订单金额
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        //6.2 封装2个集合，订单明细列表、订单减免明细列表
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);

        //6.3  封装其他属性
        //6.3.1 封装购买项目类型
        orderInfoVo.setItemType(itemType);
        //6.3.2 封装"本次订单"流水号 避免回退或连续点击提交订单导致订单重复提交
        String tradeNo = IdUtil.fastUUID();
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        redisTemplate.opsForValue().set(tradeKey, tradeNo, 5, TimeUnit.MINUTES);
        orderInfoVo.setTradeNo(tradeNo);

        //6.3.3  封装时间戳、签名
        orderInfoVo.setTimestamp(System.currentTimeMillis());
        //将订单VO转为Map 忽略付款方式payWay,不需要参与签名生成，此刻还不知道用户付款方式
        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo, false, true);
        String sign = SignHelper.getSign(map);
        orderInfoVo.setSign(sign);
        //7.返回封装完毕订单VO对象
        return orderInfoVo;*/


    }

    /**
     * 提交订单（选择支付方式 支持余额跟微信支付）
     *
     * @param orderInfoVo 订单VO信息
     * @return {orderNo: 订单编号} 前端获取到订单编号后可以跳转到微信支付页面、或跳转支付成功页面
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public Map<String, String> submitOrder(Long userId, OrderInfoVo orderInfoVo) {
        //1.业务校验：校验流水号，防止订单重复提交 采用lua脚本保证判断流水号跟删除流水号原子性
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        //1.1 定义lua脚本 KEYS[1]=流水号Key  ARGV[1]=前端提交流水号
        String luaText = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";
        // 创建脚本对象 封装lua脚本内容
        RedisScript<Boolean> redisScript = new DefaultRedisScript<>(luaText, Boolean.class);
        //1.2 执行lua脚本
        Boolean flag = (Boolean) redisTemplate.execute(redisScript, Arrays.asList(tradeKey), orderInfoVo.getTradeNo());
        if (!flag) {
            throw new SoundscapeException(500, "流水号校验失败");
        }

        //2.业务校验：校验签名，防止订单数据被前端篡改
        // 当时结算接口生成签名时候未确定支付方式将"payWay"移除，现在验签同样将"payWay"排除
        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo);
        map.remove("payWay");
        SignHelper.checkSign(map);

        //核心业务逻辑处理
        //3. 核心业务，保存订单相关数据：订单信息、订单明细、订单减免信息
        OrderInfo orderInfo = this.saveOrderInfo(userId, orderInfoVo);

        //4. 核心业务-处理付款方式：余额  立即扣减账户金额、修改订单状态（已支付）、发放响应权益
        // 支付方式：1101-微信 1102-支付宝 1103-账户余额
        String payWay = orderInfoVo.getPayWay();
        if (ORDER_PAY_ACCOUNT.equals(payWay)) {
            //4.1 远程调用"账户服务"扣减账户余额，如果有异常，一定要抛出
            //4.1.1 构建扣减余额VO对象
            AccountDeductVo accountDeductVo = new AccountDeductVo();
            accountDeductVo.setOrderNo(orderInfo.getOrderNo());
            accountDeductVo.setUserId(orderInfo.getUserId());
            accountDeductVo.setAmount(orderInfo.getOrderAmount());
            accountDeductVo.setContent(orderInfo.getOrderTitle());
            //4.1.2 远程调用完成扣减
            Result result = accountFeignClient.checkAndDeduct(accountDeductVo);
            //4.1.3 远程调用结果，判断业务状态码是否为200
            if (result.getCode().intValue() != 200) {
                throw new SoundscapeException(result.getCode(), result.getMessage());
            }
            //4.2 扣减账户余额成功，则修改订单状态为已支付
            orderInfo.setOrderStatus(ORDER_STATUS_PAID);
            orderInfoMapper.updateById(orderInfo);

            //4.3 远程调用"用户服务"权益发放 ，如果有异常，一定要抛出
            //4.3.1 构建权益发放VO对象
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
            userPaidRecordVo.setUserId(orderInfo.getUserId());
            userPaidRecordVo.setItemType(orderInfo.getItemType());
            List<Long> itemIdList = orderInfoVo.getOrderDetailVoList().stream().map(OrderDetailVo::getItemId).collect(Collectors.toList());
            userPaidRecordVo.setItemIdList(itemIdList);
            //4.3.2 远程调用完成扣减
            result = userFeignClient.savePaidRecord(userPaidRecordVo);
            //4.3.3 远程调用结果，判断业务状态码是否为200
            if (result.getCode().intValue() != 200) {
                throw new SoundscapeException(result.getCode(), result.getMessage());
            }
        }

        //5. 自动关单：15分钟未支付自动关单 采用RabbitMQ延迟消息
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER, orderInfo.getId(), cancelTTL);

        //6. 封装订单编号返回
        return Map.of("orderNo", orderInfo.getOrderNo());
    }


    /**
     * 保存订单相关信息
     *
     * @param userId      用户ID
     * @param orderInfoVo 订单VO
     * @return 订单对象
     */
    @Override
    public OrderInfo saveOrderInfo(Long userId, OrderInfoVo orderInfoVo) {
        //1. 保存订单
        //1.1 封装订单对象
        OrderInfo orderInfo = BeanUtil.copyProperties(orderInfoVo, OrderInfo.class);
        //1.2 封装订单对象其他属性
        //1.2.1 用户ID
        orderInfo.setUserId(userId);
        //1.2.2 订单标题
        String itemName = orderInfoVo.getOrderDetailVoList().get(0).getItemName();
        orderInfo.setOrderTitle(itemName);
        //1.2.3 订单编号 订单编号：年月日+雪花算法
        String orderNo = DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextIdStr();
        orderInfo.setOrderNo(orderNo);
        //1.2.4 支付状态：未支付  订单状态：0901-未支付 0902-已支付 0903-已取消"
        orderInfo.setOrderStatus(ORDER_STATUS_UNPAID);
        //1.3 保存订单，得到订单ID
        orderInfoMapper.insert(orderInfo);
        Long orderId = orderInfo.getId();

        //2. 保存订单明细
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (CollUtil.isNotEmpty(orderDetailVoList)) {
            List<OrderDetail> orderDetailList = orderDetailVoList.stream().map(vo -> {
                OrderDetail orderDetail = BeanUtil.copyProperties(vo, OrderDetail.class);
                orderDetail.setOrderId(orderId);
                return orderDetail;
            }).collect(Collectors.toList());
            //批量保存
            orderDetailService.saveBatch(orderDetailList);
        }

        //3. 保存优惠减免
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (CollUtil.isNotEmpty(orderDerateVoList)) {
            for (OrderDerateVo orderDerateVo : orderDerateVoList) {
                OrderDerate orderDerate = BeanUtil.copyProperties(orderDerateVo, OrderDerate.class);
                orderDerate.setOrderId(orderId);
                orderDerateMapper.insert(orderDerate);
            }
        }
        return orderInfo;
    }

    /**
     * 监听延迟关单消息，自动关闭超时订单
     *
     * @param orderId
     */
    @Override
    public void cancelOrder(Long orderId) {
       /* //1. 查询订单
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        String orderStatus = orderInfo.getOrderStatus();
        //2.更新订单
        if (ORDER_STATUS_UNPAID.equals(orderStatus)) {
            orderInfo.setOrderStatus(ORDER_STATUS_CANCEL);
            orderInfoMapper.updateById(orderInfo);
        }*/
        //采用乐观锁思想更新，带着期望订单状态(未支付)更新
        int update = orderInfoMapper.update(
                null,
                new LambdaUpdateWrapper<OrderInfo>()
                        .eq(OrderInfo::getId, orderId)
                        .eq(OrderInfo::getOrderStatus, ORDER_STATUS_UNPAID)
                        .set(OrderInfo::getOrderStatus, ORDER_STATUS_CANCEL)
        );
        log.info("订单ID：{}，更新结果：{}", orderId, update);
    }

    /**
     * 根据订单编号查询订单信息（包含订单明细）
     *
     * @param orderNo
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderNo) {
        //1.根据订单编号查询订单信息
        OrderInfo orderInfo = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderNo, orderNo)
        );
        Assert.notNull(orderInfo, "订单不存在");
        //2.根据订单ID查询订单明细
        Long orderInfoId = orderInfo.getId();
        List<OrderDetail> orderDetailList = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>()
                        .eq(OrderDetail::getOrderId, orderInfoId)
        );
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    /**
     * 分页查询指定用户订单列表（包含订单明细）
     *
     * @param pageInfo 分页对象
     * @param userId   用户ID
     * @return 分页对象
     */
    @Override
    public IPage<OrderInfo> findUserPage(IPage<OrderInfo> pageInfo, Long userId) {
        //1.方案一：先分页查询订单，遍历订单查询每个订单包含订单明细 背后执行SQL至少11条
        //return orderInfoMapper.findUserPage(pageInfo, userId);
        //2.方案二：先分页查询订单，根据订单ID查询订单明细
        IPage<OrderInfo> orderInfoIPage = orderInfoMapper.selectPage(
                pageInfo,
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getUserId, userId)
                        .select(OrderInfo::getId, OrderInfo::getOrderNo, OrderInfo::getOrderStatus, OrderInfo::getOriginalAmount, OrderInfo::getOrderAmount, OrderInfo::getPayWay)
        );
        //获取所有订单ID
        List<Long> orderIdList =
                orderInfoIPage.getRecords().stream().map(OrderInfo::getId).collect(Collectors.toList());
        //查询订单明细列表
        List<OrderDetail> orderDetailList = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>()
                        .in(OrderDetail::getOrderId, orderIdList)
                        .select(OrderDetail::getOrderId, OrderDetail::getId, OrderDetail::getItemName, OrderDetail::getItemPrice, OrderDetail::getItemUrl)
        );
        //按订单ID分组得到Map<订单ID，订单明细列表>
        Map<Long, List<OrderDetail>> orderDetailListMap = orderDetailList.stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));
        //为每个订单关联订单明细列表
        for (OrderInfo orderInfo : orderInfoIPage.getRecords()) {
            orderInfo.setOrderDetailList(orderDetailListMap.get(orderInfo.getId()));
        }
        return pageInfo;
    }

    /**
     * 微信支付（真实或本地模拟）成功后的订单处理。
     * 条件更新保证重复轮询不会重复发放权益。
     */
    @Override
    public void orderPaySuccess(String orderNo) {
        int updateCount = orderInfoMapper.update(
                null,
                new LambdaUpdateWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderNo, orderNo)
                        .eq(OrderInfo::getOrderStatus, ORDER_STATUS_UNPAID)
                        .set(OrderInfo::getOrderStatus, ORDER_STATUS_PAID)
        );
        if (updateCount == 0) {
            return;
        }

        OrderInfo orderInfo = this.getOrderInfo(orderNo);
        List<Long> itemIdList = orderInfo.getOrderDetailList().stream()
                .map(OrderDetail::getItemId)
                .collect(Collectors.toList());
        UserPaidRecordVo paidRecord = new UserPaidRecordVo();
        paidRecord.setOrderNo(orderNo);
        paidRecord.setUserId(orderInfo.getUserId());
        paidRecord.setItemType(orderInfo.getItemType());
        paidRecord.setItemIdList(itemIdList);
        Result result = userFeignClient.savePaidRecord(paidRecord);
        if (result == null || result.getCode().intValue() != 200) {
            throw new SoundscapeException(500, "支付权益发放失败");
        }
    }

}
