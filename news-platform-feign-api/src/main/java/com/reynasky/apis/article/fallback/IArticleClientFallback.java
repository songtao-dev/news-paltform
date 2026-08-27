package com.reynasky.apis.article.fallback;

import com.reynasky.apis.article.IArticleClient;
import com.reynasky.model.article.dtos.ArticleDto;
import com.reynasky.model.common.dtos.ResponseResult;
import com.reynasky.model.common.enums.AppHttpCodeEnum;
import org.springframework.stereotype.Component;

@Component
public class IArticleClientFallback implements IArticleClient {
    @Override
    public ResponseResult saveArticle(ArticleDto dto) {
        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR,"获取数据失败");
    }
}
