package com.reynasky.user.service;


import com.reynasky.model.common.dtos.ResponseResult;
import com.reynasky.model.user.dtos.UserRelationDto;



public interface ApUserRelationService {
    /**
     * 用户关注/取消关注
     * @param dto
     * @return
     */
    public ResponseResult follow(UserRelationDto dto);
}