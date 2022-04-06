package com.dpwgc.fastim.server;

import com.alibaba.fastjson.JSON;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.*;

/**
 * Redis订阅监听服务（监听所有IM服务器接收到的消息）
 */
@Component
public class RedisListenServer implements MessageListener {

    //concurrent包的线程安全Set，用来存放每个客户端对应的WebSocket session对象。
    private static ConcurrentHashMap<String, Session> sessionPools = new ConcurrentHashMap<>();

    //插入新的session
    public void setSession(String userId,Session session) {
        sessionPools.put(userId,session);
    }


    @Override
    public void onMessage(Message message, byte[] pattern) {

        String msg = new String(message.getBody()); //消息内容（JSON字符串）
        String topic = new String(pattern);         //消息主题（mq:123456，"123456"为群组id）

        //遍历当前在线的会话key列表
        for (String key: sessionPools.keySet()) {

            //根据key获取value
            Session session = sessionPools.get(key);

            //如果会话不存在或者已关闭
            if(null == session || !session.isOpen()) {
                //删除并跳过
                sessionPools.remove(key);
                continue;
            }

            try {
                //如果消息与会话属于同一群组
                if(session.getPathParameters().get("groupId").equals(topic.split("mq:")[1])){
                    synchronized (session) {
                        //推送消息
                        session.getBasicRemote().sendText(JSON.parse(msg).toString());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}