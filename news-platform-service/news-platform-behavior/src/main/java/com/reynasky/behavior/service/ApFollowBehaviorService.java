package com.reynasky.behavior.service;

import com.reynasky.model.behavior.dtos.FollowBehaviorDto;

public interface ApFollowBehaviorService {

    /**
     * 保存关注行为
     * @param dto
     * @return
     */
    public boolean saveFollowBehavior(FollowBehaviorDto dto);
}
