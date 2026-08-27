package com.reynasky.search.service;

import com.reynasky.model.common.dtos.ResponseResult;
import com.reynasky.model.search.dtos.UserSearchDto;

public interface ApAssociateWordsService {

    /**
     * 搜索联想词
     * @param dto
     * @return
     */
    public ResponseResult search(UserSearchDto dto);
}
