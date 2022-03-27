package com.dpwgc.fastim.service;

import com.dpwgc.fastim.dao.MessageObject;
import com.dpwgc.fastim.util.RedisUtil;
import com.dpwgc.fastim.util.ResultUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ObjectMapper objectMapper;

    //消息过期清除时限
    @Value("${im.recallTimeLimit}")
    long recallTimeLimit;

    /**
     * 根据群组id返回该群组的消息列表
     * @param groupId 群组id
     * @param startPage 起始页
     * @param endPage 终止页
     * @return ResultUtil<Object>
     */
    public ResultUtil<Object> listMessage(String groupId,long startPage,long endPage){

        ResultUtil<Object> resultUtil = new ResultUtil<>();

        //获取指定区间的消息列表
        List<Object> list = redisUtil.lGet(groupId,startPage,endPage);

        resultUtil.setCode(200);
        resultUtil.setMsg("操作成功");
        resultUtil.setData(list);
        return resultUtil;
    }

    /**
     * 根据用户id、群组id、及消息创建时间戳撤回指定消息
     * @param userId 用户id
     * @param groupId 群组id
     * @param ts 消息创建时间戳（毫秒级）
     * @return ResultUtil<Object>
     */
    public ResultUtil<Object> delMessage(String userId, String groupId, Long ts){

        ResultUtil<Object> resultUtil = new ResultUtil<>();

        //如果当前时间超过了消息撤回时限
        if(System.currentTimeMillis() > ts + (recallTimeLimit*1000)){
            resultUtil.setCode(100);
            resultUtil.setMsg("无法撤回消息");
            return resultUtil;
        }

        //根据群组id获取群组全部消息
        List<Object> list = redisUtil.lGet(groupId,0,-1);

        //遍历群组消息
        for (Object msg : list) {

            //将JSON字符串转为消息模板对象
            MessageObject messageObject = objectMapper.convertValue(msg, MessageObject.class);

            //通过比对用户id及消息创建时间戳查找到指定消息
            if (messageObject.getUserId().equals(userId) && messageObject.getTs().equals(ts)) {

                //从Redis list中删除这一个消息
                long i = redisUtil.lRemove(groupId, 1, msg);
                if(i == 0){
                    resultUtil.setCode(100);
                    resultUtil.setMsg("消息不存在");
                    return resultUtil;
                }
            }
        }

        resultUtil.setCode(200);
        resultUtil.setMsg("撤回消息成功");
        return resultUtil;
    }
}
