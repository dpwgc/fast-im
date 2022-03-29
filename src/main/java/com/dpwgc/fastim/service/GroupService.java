package com.dpwgc.fastim.service;

import com.alibaba.fastjson.JSON;
import com.dpwgc.fastim.dao.MessageObject;
import com.dpwgc.fastim.util.RedisUtil;
import com.dpwgc.fastim.util.ResultUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 群组类服务
 */
@Service
public class GroupService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ObjectMapper objectMapper;

    //消息过期清除时限
    @Value("${im.groupMessage.recallTimeLimit}")
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
        List<Object> list = redisUtil.lGet("gml:"+groupId,startPage,endPage);

        resultUtil.setCode(200);
        resultUtil.setMsg("操作成功");
        resultUtil.setData(list);
        return resultUtil;
    }

    /**
     * 用户根据用户id、群组id、及消息创建时间戳获取指定消息之前的历史消息列表（同步历史消息）
     * @param userId 用户id
     * @param groupId 群组id
     * @param ts 消息创建时间戳（毫秒级）
     * @param count 要获取的消息数量
     * @return ResultUtil<Object>
     */
    public ResultUtil<Object> listOldMessage(String userId, String groupId, Long ts,long count){

        ResultUtil<Object> resultUtil = new ResultUtil<>();

        long startPage; //起始页
        long endPage;   //终止页

        //根据群组id获取群组全部消息
        List<Object> list = redisUtil.lGet("gml:"+groupId,0,-1);
        //指定消息之前的历史消息列表
        List<Object> oldList = null;

        for(int i=0;i<list.size();i++) {

            //将Object转为MessageObject
            MessageObject messageObject = JSON.parseObject(list.get(i).toString(), MessageObject.class);

            //通过比对用户id及消息创建时间戳查找到指定消息
            if (messageObject.getUserId().equals(userId) && messageObject.getTs().equals(ts)) {

                //将终止页设在这条消息索引的前一位
                endPage = i - 1;

                //将起始页设在终止页的前{count}位
                startPage = endPage-count;

                //如果计算得到的起始页小于0
                if(startPage < 0) {
                    //将起始位设为0（即列表开头）
                    startPage = 0;
                }
                //获取起始页与终止页之间的历史消息列表
                oldList = redisUtil.lGet("gml:"+groupId,startPage,endPage);
                break;
            }
        }

        resultUtil.setCode(200);
        resultUtil.setMsg("获取成功");
        resultUtil.setData(oldList);
        return resultUtil;
    }

    /**
     * 用户根据自己的id、群组id、及消息创建时间戳撤回指定消息
     * @header userId 用户id
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
        List<Object> list = redisUtil.lGet("gml:"+groupId,0,-1);

        //遍历群组消息
        for (Object msg : list) {

            //将Object转为MessageObject
            MessageObject messageObject = JSON.parseObject(msg.toString(), MessageObject.class);

            //通过比对用户id及消息创建时间戳查找到指定消息
            if (messageObject.getUserId().equals(userId) && messageObject.getTs().equals(ts)) {

                //从Redis list中删除这一个消息
                long i = redisUtil.lRemove("gml:"+groupId, 1, msg);
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
