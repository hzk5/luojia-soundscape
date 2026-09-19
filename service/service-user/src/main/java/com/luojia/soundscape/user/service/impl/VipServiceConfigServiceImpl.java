package com.luojia.soundscape.user.service.impl;

import com.luojia.soundscape.model.user.VipServiceConfig;
import com.luojia.soundscape.user.mapper.VipServiceConfigMapper;
import com.luojia.soundscape.user.service.VipServiceConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"all"})
public class VipServiceConfigServiceImpl extends ServiceImpl<VipServiceConfigMapper, VipServiceConfig> implements VipServiceConfigService {

	@Autowired
	private VipServiceConfigMapper vipServiceConfigMapper;


}
