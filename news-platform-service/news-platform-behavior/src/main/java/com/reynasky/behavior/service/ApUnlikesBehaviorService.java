package com.reynasky.behavior.service;

import com.reynasky.model.behavior.dtos.UnLikesBehaviorDto;
import com.reynasky.model.common.dtos.ResponseResult;

/**
 * <p>
 * APP不喜欢行为表 服务类
 * </p>
 *
 * @author Reyna-sky
 */
public interface ApUnlikesBehaviorService {

    /**
     * 不喜欢
     * @param dto
     * @return
     */
    public ResponseResult unLike(UnLikesBehaviorDto dto);

}