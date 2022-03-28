package com.dpwgc.fastim.service;

import com.dpwgc.fastim.util.RedisUtil;
import com.dpwgc.fastim.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * 用户类服务
 */
@Service
public class UserService {

    @Autowired
    RedisUtil redisUtil;

    /**
     * 用户登录（根据业务自定义）
     * @param userId 用户id
     * @param password 用户密码
     * @return ResultUtil<Object>
     */
    public ResultUtil<Object> login(String userId,String password) {

        ResultUtil<Object> resultUtil = new ResultUtil<>();

        System.out.println(userId);
        System.out.println(password);

        /* 自定义业务处理 */
        /* --- TODO --- */
        /* 自定义业务处理 */

        //生成token
        String token = UUID.randomUUID().toString().replaceAll("-","");

        // 将token存入Redis，设置24小时过期
        if(redisUtil.set("login:"+userId, token, 60 * 60 * 24)){
            resultUtil.setCode(200);
            resultUtil.setMsg(token);           //返回token
            resultUtil.setData("{user_info}");  //返回用户信息
            return resultUtil;
        }

        resultUtil.setCode(100);
        resultUtil.setMsg("登录失败");
        return resultUtil;
    }

    /**
     * 用户加入群组
     * @param userId 用户id
     * @param groupId 群组id
     * @return ResultUtil<Object>
     */
    public ResultUtil<Object> joinGroup(String userId,String groupId) {

        ResultUtil<Object> resultUtil = new ResultUtil<>();

        //将该群组id添加进用户所加入的群组集合
        long i = redisUtil.sSet("ugs:"+userId,groupId);
        if(i>0){
            resultUtil.setCode(200);
            resultUtil.setMsg("操作成功");
            return resultUtil;
        }
        resultUtil.setCode(100);
        resultUtil.setMsg("操作失败");
        return resultUtil;
    }

    /**
     * 获取用户所加入的群组集合
     * @param userId 用户id
     * @return ResultUtil<Object>
     */
    public ResultUtil<Object> listGroup(String userId) {

        ResultUtil<Object> resultUtil = new ResultUtil<>();

        //获取用户所加入的群组id集合
        Set<Object> set = redisUtil.sGet("ugs:"+userId);

        resultUtil.setCode(200);
        resultUtil.setMsg("操作成功");
        resultUtil.setData(set);
        return resultUtil;
    }
}
