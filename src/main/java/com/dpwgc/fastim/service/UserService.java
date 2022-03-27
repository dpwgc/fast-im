package com.dpwgc.fastim.service;

import com.dpwgc.fastim.util.RedisUtil;
import com.dpwgc.fastim.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    RedisUtil redisUtil;

    public ResultUtil<Object> join(String userId,String groupId) {

        ResultUtil<Object> resultUtil = new ResultUtil<>();

        //将该群组id添加进用户所加入的群组集合
        long i = redisUtil.sSet("ugs"+userId,groupId);
        if(i>0){
            resultUtil.setCode(200);
            resultUtil.setMsg("操作成功");
            return resultUtil;
        }
        resultUtil.setCode(100);
        resultUtil.setMsg("操作失败");
        return resultUtil;
    }
}
