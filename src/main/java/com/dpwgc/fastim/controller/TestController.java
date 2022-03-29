package com.dpwgc.fastim.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/test")
public class TestController {

    /**
     * WebSocket连接测试页面 http://127.0.0.1:9000/test/websocket
     * @return String
     */
    @GetMapping("/websocket")
    public String websocket(){

        return "test";
    }
}
