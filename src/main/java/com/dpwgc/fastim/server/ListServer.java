package com.dpwgc.fastim.server;

import com.alibaba.fastjson.JSONObject;
import com.dpwgc.fastim.config.IMConfig;
import com.dpwgc.fastim.dao.Group;
import com.dpwgc.fastim.dao.MessageList;
import com.dpwgc.fastim.dao.MessageObject;
import com.dpwgc.fastim.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 首页群组列表连接（监听用户所加入的群组列表数据更新）
 */
@ServerEndpoint("/list/{userId}")
@Component
public class ListServer {

    //Redis工具类
    private static RedisUtil redisUtil;

    //IM配置信息加载
    private static IMConfig imConfig;

    //是否继续监听
    private volatile boolean flag;
    //当前用户ID
    private volatile String userId;

    @Autowired
    public void setRepository(RedisUtil redisUtil) {
        ListServer.redisUtil = redisUtil;
    }

    @Autowired
    public void setRepository(IMConfig imConfig) {
        ListServer.imConfig = imConfig;
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

    //给指定用户发送信息
    public static void sendInfo(String userId, String message){
        Session session = sessionPools.get(userId);
        try {
            sendMessage(session, message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //群组监听线程
    Thread thread = new Thread(){
        @Override
        public void run() {

            while (flag) {
                //获取用户加入的群组集合
                Set<Object> set = redisUtil.sGet("ugs:"+userId);
                //集合转数组
                Object[] arr = set.toArray();
                //群组列表
                List<Group> groups = new ArrayList<>();

                //遍历数组，获取群组最新消息总数和最新消息
                for(int i=0;i<set.size();i++){
                    //获取群组消息总数
                    long size = redisUtil.lGetListSize("gml:"+arr[i].toString());
                    //获取群组最新消息
                    Object msg = redisUtil.lGetIndex("gml:"+arr[i].toString(),size);

                    //将数据添加进群组列表
                    Group group = new Group();
                    group.setNewMessage(msg);
                    group.setTotal(size);
                    groups.add(group);
                }

                sendInfo(userId,groups.toString());

                //每隔一段时间更新一次
                try {
                    Thread.sleep(imConfig.getUpdateRate());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * 建立连接成功调用
     * @param session 会话
     * @param userId 用户id
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId){

        sessionPools.put(userId, session);//添加用户
        addOnlineCount();

        this.userId=userId;
        this.flag=true;

        //开始群组监听进程
        thread.start();
    }

    /**
     * 关闭连接时调用
     * @param userId 用户id
     */
    @OnClose
    public void onClose(@PathParam(value = "userId") String userId){
        sessionPools.remove(userId);//删除用户

        //关闭进程
        flag=false;
        thread.interrupt();

        subOnlineCount();
    }

    //错误时调用
    @OnError
    public void onError(Session session, Throwable throwable) throws IOException {
        throwable.printStackTrace();

        //关闭进程
        flag=false;
        thread.interrupt();

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
