package com.reynasky.user.service.impl;

import com.reynasky.common.redis.CacheService;
import com.reynasky.model.common.dtos.ResponseResult;
import com.reynasky.model.common.enums.AppHttpCodeEnum;
import com.reynasky.model.user.dtos.UserRelationDto;
import com.reynasky.model.user.pojos.ApUser;
import com.reynasky.user.service.ApUserRelationService;
import com.reynasky.utils.thread.AppThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApUserRelationServiceImpl implements ApUserRelationService {

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    private CacheService cacheService;


    /**
     * 用户关注/取消关注
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult follow(UserRelationDto dto) {
        //1 参数校验
        if (dto.getOperation() == null ||
                dto.getOperation() < 0 || dto.getOperation() > 1) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2 判断是否登录
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        Integer apUserId = user.getId();

        //3 关注 apuser:follow:  apuser:fans:
        Integer followUserId = dto.getAuthorId();
        if (dto.getOperation() == 0) {
            // 将对方写入我的关注中
            cacheService.zAdd("apuser:follow:" + apUserId, followUserId.toString(), System.currentTimeMillis());
            // 将我写入对方的粉丝中
            cacheService.zAdd("apuser:fans:" + followUserId, apUserId.toString(), System.currentTimeMillis());

        } else {
            // 取消关注
            cacheService.zRemove("apuser:follow:" + apUserId, followUserId.toString());
            cacheService.zRemove("apuser:fans:" + followUserId, apUserId.toString());
        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

    }
}