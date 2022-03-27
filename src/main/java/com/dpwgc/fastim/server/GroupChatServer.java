package com.dpwgc.fastim.server;

import com.alibaba.fastjson.JSONObject;
import com.dpwgc.fastim.config.IMConfig;
import com.dpwgc.fastim.dao.MessageList;
import com.dpwgc.fastim.dao.MessageObject;
import com.dpwgc.fastim.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
@ServerEndpoint("/group/chat/{groupId}/{userId}")
@Component
public class GroupChatServer {

    //Redis工具类
    private static RedisUtil redisUtil;

    //IM配置信息加载
    private static IMConfig imConfig;

    @Autowired
    public void setRepository(RedisUtil redisUtil) {
        GroupChatServer.redisUtil = redisUtil;
    }

    @Autowired
    public void setRepository(IMConfig imConfig) {
        GroupChatServer.imConfig = imConfig;
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

        try {
            //list的总长度（终止页）
            long endPage = redisUtil.lGetListSize("gml:"+groupId);

            //单次获取的消息数量
            long listNum = imConfig.getListNum();

            //计算起始页
            long startPage = endPage-listNum;

            //如果起始页小于0，将起始页设为0
            if(startPage < 0){
                startPage = 0;
            }

            //更新用户加入的群组集合
            redisUtil.sSet("ugs:"+userId,groupId);

            //连接建立后返回{listNum}条消息（从list右侧开始输出，获取最新的{listNum}条数据）
            List<Object> list = redisUtil.lGet("gml:"+groupId,startPage,endPage);

            //封装成MessageList类型
            MessageList messageList = new MessageList();
            messageList.setLimitList(list);//返回list的部分消息
            messageList.setTotal(endPage);//返回redis list的总长度

            //发送消息
            sendMessage(session, messageList.toString());

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
    }

    /**
     * 收到客户端信息
     * @param message 消息主体内容
     * @param userId 用户id
     * @param groupId 群组id
     */
    @OnMessage
    public void onMessage(String message,@PathParam(value = "userId") String userId,@PathParam(value = "groupId") String groupId) {

        //创建消息模板
        MessageObject messageObject = new MessageObject();
        messageObject.setUserId(userId);
        messageObject.setInfo(message);
        messageObject.setTs(System.currentTimeMillis());

        //将MessageObject对象转为json字符串
        String jsonStr = JSONObject.toJSON(messageObject).toString();

        //将消息插入Redis list中（key为groupId）
        redisUtil.lSet("gml:"+groupId,jsonStr,imConfig.getTimeout());

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
    public void onError(Session session, Throwable throwable) throws IOException {
        throwable.printStackTrace();
        //关闭对话
        session.close();
    }

    public static void addOnlineCount(){
        onlineNum.incrementAndGet();
    }

    public static void subOnlineCount() {
        onlineNum.decrementAndGet();
    }
}
