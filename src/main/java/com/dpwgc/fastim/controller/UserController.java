package com.dpwgc.fastim.controller;

import com.dpwgc.fastim.service.UserService;
import com.dpwgc.fastim.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    UserService userService;

    /**
     * 用户加入群组
     * @param userId 用户id
     * @param groupId 群组id
     * @return
     */
    @PostMapping("/join")
    public ResultUtil<Object> join(@RequestParam("userId") String userId,
                                   @RequestParam("groupId") String groupId) {

        return userService.join(userId,groupId);
    }
}
