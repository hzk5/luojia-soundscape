package com.luojia.soundscape.user.service.impl;

import com.luojia.soundscape.model.user.UserPaidTrack;
import com.luojia.soundscape.user.mapper.UserPaidAlbumMapper;
import com.luojia.soundscape.user.mapper.UserPaidTrackMapper;
import com.luojia.soundscape.user.service.UserPaidTrackService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"all"})
public class UserPaidTrackServiceImpl extends ServiceImpl<UserPaidTrackMapper, UserPaidTrack> implements UserPaidTrackService {

	@Autowired
	private UserPaidAlbumMapper userPaidAlbumMapper;

}
