package com.reynasky.behavior.service;

import com.reynasky.model.behavior.dtos.LikesBehaviorDto;
import com.reynasky.model.common.dtos.ResponseResult;

public interface ApLikesBehaviorService {

    /**
     * 存储喜欢数据
     * @param dto
     * @return
     */
    public ResponseResult like(LikesBehaviorDto dto);
}
