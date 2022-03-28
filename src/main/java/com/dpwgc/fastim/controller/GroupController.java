package com.dpwgc.fastim.controller;

import com.dpwgc.fastim.service.GroupService;
import com.dpwgc.fastim.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 群组类接口
 */
@CrossOrigin
@RestController
@RequestMapping(value = "/group")
public class GroupController {

    @Autowired
    GroupService groupService;

    /**
     * 根据群组id返回该群组指定区间的消息列表
     * @param groupId 群组id
     * @param startPage 起始页
     * @param endPage 终止页
     * @return ResultUtil<Object>
     */
    @PostMapping(value = "/listMessage")
    public ResultUtil<Object> listMessage(@RequestParam("groupId") String groupId,
                                          @RequestParam("startPage") long startPage,
                                          @RequestParam("endPage") long endPage) {

        return groupService.listMessage(groupId,startPage,endPage);
    }

    /**
     * 用户根据用户id、群组id、及消息创建时间戳撤回指定消息
     * @header userId 用户id
     * @param groupId 群组id
     * @param ts 消息创建时间戳（毫秒级）
     * @return ResultUtil<Object>
     */
    @PostMapping(value = "/delMessage")
    public ResultUtil<Object> delMessage(@RequestHeader("userId") String userId,
                                         @RequestParam("groupId") String groupId,
                                         @RequestParam("ts") Long ts) {

        return groupService.delMessage(userId,groupId,ts);
    }
}
