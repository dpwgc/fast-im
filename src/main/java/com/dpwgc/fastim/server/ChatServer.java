package com.dpwgc.fastim.server;

import com.alibaba.fastjson.JSONObject;
import com.dpwgc.fastim.dao.MessageObject;
import com.dpwgc.fastim.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 群组聊天室连接（监听群内聊天消息更新）
 */
@ServerEndpoint("/chat/{groupId}/{userId}")
public class ChatServer {

    //消息过期清除时限
    @Value("${message.timeout}")
    long timeout;
    //每次请求获取的消息数量
    @Value("${message.listNum}")
    int listNum;

    //Redis工具类
    private static RedisUtil redisUtil;

    @Autowired
    public void setRepository(RedisUtil redisUtil) {
        ChatServer.redisUtil = redisUtil;
    }

    //静态变量，用来记录当前在线连接数,线程安全。
    private static AtomicInteger onlineNum = new AtomicInteger();

    //concurrent包的线程安全Set，用来存放每个客户端对应的WebSocketServer对象。
    //消息通道
    private static ConcurrentHashMap<String, Session> sessionPools = new ConcurrentHashMap<>();

    //发送消息
    public static void sendMessage(Session session, String message) throws IOException {
        if(session != null){
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        }
    }

    /**
     * 建立连接成功调用
     * @param session 会话
     * @param userId 用户id
     * @param groupId 群组id
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId,@PathParam(value = "groupId") String groupId){

        sessionPools.put(userId, session);//添加用户
        addOnlineCount();

        System.out.println(userId + "加入群组"+groupId+"，当前在线人数为" + onlineNum);
        try {
            //连接建立后返回前{listNum}条消息
            List<Object> list = redisUtil.lGet(groupId,0,listNum); //TODO listNum无法读取
            sendMessage(session, list.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭连接时调用
     * @param userId 用户id
     */
    @OnClose
    public void onClose(@PathParam(value = "userId") String userId){
        sessionPools.remove(userId);//删除用户
        subOnlineCount();
        System.out.println(userId + "断开WebSocket连接，当前在线人数为" + onlineNum);
    }

    /**
     * 收到客户端信息
     * @param message 消息主体内容
     * @param userId 用户id
     * @param groupId 群组id
     */
    @OnMessage
    public void onMessage(String message,@PathParam(value = "userId") String userId,@PathParam(value = "groupId") String groupId) {

        //创建消息模板对象
        MessageObject messageObject = new MessageObject();
        messageObject.setUserId(userId);
        messageObject.setInfo(message);
        messageObject.setTs(System.currentTimeMillis());

        //将MessageObject对象转为json字符串
        String jsonStr = JSONObject.toJSON(messageObject).toString();

        //将消息插入Redis list中（key为groupId）
        redisUtil.lSet(groupId,jsonStr,timeout);

        //广播推送消息
        for (Session session: sessionPools.values()) {
            try {
                sendMessage(session, jsonStr);
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    //错误时调用
    @OnError
    public void onError(Session session, Throwable throwable){
        System.out.println("error");
        throwable.printStackTrace();
    }

    public static void addOnlineCount(){
        onlineNum.incrementAndGet();
    }

    public static void subOnlineCount() {
        onlineNum.decrementAndGet();
    }
}
