package com.reynasky.behavior.service;

import com.reynasky.model.behavior.dtos.ReadBehaviorDto;
import com.reynasky.model.common.dtos.ResponseResult;

public interface ApReadBehaviorService {

    /**
     * 保存阅读行为
     * @param dto
     * @return
     */
    public ResponseResult readBehavior(ReadBehaviorDto dto);
}
