package com.cn.app.chatgptbot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cn.app.chatgptbot.dao.RefuelingKitDao;
import com.cn.app.chatgptbot.model.RefuelingKit;
import com.cn.app.chatgptbot.service.IRefuelingKitService;
import com.cn.app.chatgptbot.uitls.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service("RefuelingKitService")
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class RefuelingKitServiceImpl extends ServiceImpl<RefuelingKitDao, RefuelingKit> implements IRefuelingKitService {

    @Override
    public Long getUserKitId() {
        Long kitId = this.baseMapper.getUserKitId(JwtUtil.getUserId());
        return  kitId == null ? 0 : kitId;
    }
}
