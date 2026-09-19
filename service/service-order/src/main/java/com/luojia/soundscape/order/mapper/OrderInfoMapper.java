package com.luojia.soundscape.order.mapper;

import com.luojia.soundscape.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    //查询用户订单
    IPage<OrderInfo> findUserPage(IPage<OrderInfo> pageInfo, @Param("userId") Long userId);
}
