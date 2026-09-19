package com.luojia.soundscape.order.service.impl;

import com.luojia.soundscape.model.order.OrderDetail;
import com.luojia.soundscape.order.mapper.OrderDetailMapper;
import com.luojia.soundscape.order.service.OrderDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Luojia Soundscape Contributors
 */
@Slf4j
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {
}
