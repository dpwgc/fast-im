package com.dpwgc.fastim.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginUtil {

    @Autowired
    RedisUtil redisUtil;

    /**
     * 用户登录状态检查
     * @param userId 用户id
     * @param token 登录令牌
     * @return boolean
     */
    public boolean loginCheck(String userId,String token) {
        if (userId==null||token==null||!redisUtil.hasKey("login:"+userId)){
            return false;
        }
        String redis_token = (String) redisUtil.get("login:"+userId);
        if (!token.equals(redis_token)){
            return false;
        }
        //验证成功，token存活时间延长24小时
        redisUtil.set("login:"+userId, token, 60 * 60 * 24);
        return true;
    }
}
