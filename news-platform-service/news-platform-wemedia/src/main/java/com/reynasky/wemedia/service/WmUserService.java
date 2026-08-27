package com.reynasky.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reynasky.model.common.dtos.ResponseResult;
import com.reynasky.model.wemedia.dtos.WmLoginDto;
import com.reynasky.model.wemedia.pojos.WmUser;

public interface WmUserService extends IService<WmUser> {

    /**
     * 自媒体端登录
     * @param dto
     * @return
     */
    public ResponseResult login(WmLoginDto dto);

}