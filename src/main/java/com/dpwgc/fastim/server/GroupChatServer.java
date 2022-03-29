package com.dpwgc.fastim.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dpwgc.fastim.config.IMConfig;
import com.dpwgc.fastim.dao.MessageList;
import com.dpwgc.fastim.dao.MessageObject;
import com.dpwgc.fastim.util.LoginUtil;
import com.dpwgc.fastim.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 群组聊天室连接（监听群内聊天消息更新）
 */
@ServerEndpoint("/group/chat/{groupId}/{userId}/{token}")
@Component
public class GroupChatServer {

    //Redis工具类（交由IOC自动注入）
    private static RedisUtil redisUtil;

    //IM配置信息加载（交由IOC自动注入）
    private static IMConfig imConfig;

    //登录状态检查（交由IOC自动注入）
    private static LoginUtil loginUtil;

    //Redis订阅监听器设置（交由IOC自动注入）
    private static RedisMessageListenerContainer redisMessageListenerContainer;

    @Autowired
    public void setRepository(RedisUtil redisUtil) {
        GroupChatServer.redisUtil = redisUtil;
    }

    @Autowired
    public void setRepository(IMConfig imConfig) {
        GroupChatServer.imConfig = imConfig;
    }

    @Autowired
    public void setRepository(LoginUtil loginUtil) {
        GroupChatServer.loginUtil = loginUtil;
    }

    @Autowired
    public void setRepository(RedisMessageListenerContainer redisMessageListenerContainer) {
        GroupChatServer.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    //Redis订阅推送消息服务（设为静态，仅在本类加载时调用一次）
    private static RedisListenServer redisListenServer = new RedisListenServer();

    //concurrent包的线程安全Set，用来存放每个客户端对应的WebSocketServer对象。
    private static ConcurrentHashMap<String, Session> sessionPools = new ConcurrentHashMap<>();

    //给指定用户发送信息
    public static void sendInfo(String userId, String message){
        Session session = sessionPools.get(userId);
        try {
            sendMessage(session, message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

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
    public void onOpen(Session session,@PathParam(value = "token") String token, @PathParam(value = "userId") String userId,@PathParam(value = "groupId") String groupId) throws IOException {

        sessionPools.put(userId, session);//添加用户

        //如果开启了用户登录状态检测
        if(imConfig.getLoginAuth() == 1) {
            //验证用户登录状态
            if(!loginUtil.loginCheck(userId,token)){

                //验证失败，向客户端发送440状态码
                sendInfo(userId,"440");

                sessionPools.remove(userId);//删除用户
                session.close();//断开连接
                return;
            }
        }

        try {

            //是否自动加群
            if(imConfig.getAutoJoin() == 1) {
                //更新用户加入的群组集合（如果用户在此前没有加入该群组，则自动将该群组添加进用户群组集合）
                redisUtil.sSet("ugs:"+userId,groupId);
            }

            //获取用户加入的群组集合
            Set<Object> set = redisUtil.sGet("ugs:"+userId);
            //集合转数组
            Object[] arr = set.toArray();
            //遍历用户加入的群组列表，查看用户是否加入该群
            for(int i=0;i<set.size();i++){

                //如果用户已经加入该群聊
                if(arr[i].toString().equals(groupId)){
                    break;
                }

                //如果遍历到末尾还未匹配到群号，则判定用户没有加入该群
                if(i == set.size()-1) {
                    sessionPools.remove(userId);//删除用户
                    session.close();//断开连接
                    return;
                }
            }

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

            //连接建立后返回{listNum}条消息（从list右侧截取消息列表，获取最新的{listNum}条数据）
            List<Object> list = redisUtil.lGet("gml:"+groupId,startPage,endPage);

            //封装成MessageList类型
            MessageList messageList = new MessageList();
            messageList.setList(list);//返回list的部分消息
            messageList.setTotal(endPage);//返回redis list的总长度

            //发送消息（序列化发送）
            sendMessage(session, JSON.parse(messageList.toString()).toString());

            //放入session
            redisListenServer.setSession(userId,session);

            //设置监听器，监听推送该群组的消息
            redisMessageListenerContainer.addMessageListener(redisListenServer,new PatternTopic("mq:"+groupId));

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
    }

    /**
     * 收到客户端信息
     * @param message 消息主体内容
     * @param userId 用户id
     * @param groupId 群组id
     */
    @OnMessage
    public void onMessage(String message,@PathParam(value = "token") String token, @PathParam(value = "userId") String userId,@PathParam(value = "groupId") String groupId) {

        //如果开启了用户登录状态检测
        if(imConfig.getLoginAuth() == 1) {
            //验证用户登录状态
            if(!loginUtil.loginCheck(userId,token)){

                //验证失败，向客户端发送440状态码
                sendInfo(userId,"440");

                sessionPools.remove(userId);//删除用户
                return;
            }
        }

        //创建消息模板
        MessageObject messageObject = new MessageObject();
        messageObject.setGroupId(groupId);
        messageObject.setUserId(userId);
        messageObject.setInfo(message);
        messageObject.setTs(System.currentTimeMillis());

        //将MessageObject对象转为json字符串
        String jsonStr = JSONObject.toJSON(messageObject).toString();

        //将消息插入Redis list中（key为groupId）
        redisUtil.lSet("gml:"+groupId,jsonStr,imConfig.getTimeout());

        //在redis中发布消息
        redisUtil.pub("mq:"+groupId,jsonStr);
    }

    //错误时调用
    @OnError
    public void onError(Session session, Throwable throwable) throws IOException {
        throwable.printStackTrace();
        //关闭对话
        session.close();
    }
}
