package com.reynasky.apis.wemedia;

import com.reynasky.model.common.dtos.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("news-platform-wemedia")
public interface IWemediaClient {

    @GetMapping("/api/v1/channel/list")
    public ResponseResult getChannels();
}
