package com.reynasky.apis.article;

import com.reynasky.apis.article.fallback.IArticleClientFallback;
import com.reynasky.model.article.dtos.ArticleDto;
import com.reynasky.model.common.dtos.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "news-platform-article",fallback = IArticleClientFallback.class)
public interface IArticleClient {

    @PostMapping("/api/v1/article/save")
    public ResponseResult saveArticle(@RequestBody ArticleDto dto);
}
