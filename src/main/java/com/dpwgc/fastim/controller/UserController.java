package com.dpwgc.fastim.controller;

import com.dpwgc.fastim.service.UserService;
import com.dpwgc.fastim.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户类接口
 */
@CrossOrigin
@RestController
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    UserService userService;

    /**
     * 用户加入群组
     * @param userId 用户id
     * @param groupId 群组id
     * @return ResultUtil<Object>
     */
    @PostMapping("/joinGroup")
    public ResultUtil<Object> joinGroup(@RequestParam("userId") String userId,
                                        @RequestParam("groupId") String groupId) {

        return userService.joinGroup(userId,groupId);
    }

    /**
     * 获取用户所加入的群组集合
     * @param userId 用户id
     * @return ResultUtil<Object>
     */
    @PostMapping("/listGroup")
    public ResultUtil<Object> listGroup(@RequestParam("userId") String userId) {

        return userService.listGroup(userId);
    }
}
