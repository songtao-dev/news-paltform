package com.reynasky.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reynasky.model.common.dtos.ResponseResult;
import com.reynasky.model.user.dtos.LoginDto;
import com.reynasky.model.user.pojos.ApUser;

public interface ApUserService extends IService<ApUser> {
    /**
     * app端登录功能
     * @param dto
     * @return
     */
    public ResponseResult login(LoginDto dto);
}
