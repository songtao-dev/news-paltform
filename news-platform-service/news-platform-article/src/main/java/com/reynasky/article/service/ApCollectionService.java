package com.reynasky.article.service;

import com.reynasky.model.article.dtos.CollectionBehaviorDto;
import com.reynasky.model.common.dtos.ResponseResult;

public interface ApCollectionService {

    /**
     * 收藏
     * @param dto
     * @return
     */
    public ResponseResult collection(CollectionBehaviorDto dto);
}
