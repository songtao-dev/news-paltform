package com.reynasky.search.service;

import com.reynasky.model.common.dtos.ResponseResult;
import com.reynasky.model.search.dtos.UserSearchDto;

import java.io.IOException;

public interface ArticleSearchService {

    /**
     * es文章分页检索
     * @param dto
     * @return
     */
    public ResponseResult search(UserSearchDto dto) throws IOException;
}
