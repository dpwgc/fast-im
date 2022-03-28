package com.dpwgc.fastim.server;

import com.dpwgc.fastim.config.IMConfig;
import com.dpwgc.fastim.dao.GroupObject;
import com.dpwgc.fastim.util.LoginUtil;
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
 * 首页群组列表连接（监听用户加入的所有群组数据更新）
 */
@ServerEndpoint("/group/list/{userId}/{token}")
@Component
public class GroupListServer {

    //Redis工具类
    private static RedisUtil redisUtil;

    //IM配置信息加载
    private static IMConfig imConfig;

    //登录状态检查
    private static LoginUtil loginUtil;

    //是否继续监听
    private volatile boolean flag;
    //当前用户ID
    private volatile String userId;

    @Autowired
    public void setRepository(RedisUtil redisUtil) {
        GroupListServer.redisUtil = redisUtil;
    }

    @Autowired
    public void setRepository(IMConfig imConfig) {
        GroupListServer.imConfig = imConfig;
    }

    @Autowired
    public void setRepository(LoginUtil loginUtil) {
        GroupListServer.loginUtil = loginUtil;
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

            //上一次循环各个群组的消息总数之和
            long allGroupMsgTotal_ed = 0;

            while (flag) {
                //获取用户加入的群组集合
                Set<Object> set = redisUtil.sGet("ugs:"+userId);
                //集合转数组
                Object[] arr = set.toArray();
                //群组对象列表
                List<GroupObject> groupObjects = new ArrayList<>();

                //各个群组的消息总数之和
                long allGroupMsgTotal = 0;

                //遍历数组，获取每个群组的最新消息总数和最新消息
                for(int i=0;i<set.size();i++){
                    //获取群组消息总数
                    long total = redisUtil.lGetListSize("gml:"+arr[i].toString());
                    //获取群组最新消息（即list最末端元素）
                    Object msg = redisUtil.lGetIndex("gml:"+arr[i].toString(),-1);

                    //消息总数累加
                    allGroupMsgTotal = allGroupMsgTotal + total;

                    //将数据添加进群组对象列表
                    GroupObject groupObject = new GroupObject();
                    groupObject.setNewMessage(msg);
                    groupObject.setTotal(total);
                    groupObjects.add(groupObject);
                }

                //如果所有群聊的消息总数发生了变化，则表明用户群组列表中有新的消息到达
                if(allGroupMsgTotal != allGroupMsgTotal_ed) {

                    //更新allGroupMsgTotal_ed
                    allGroupMsgTotal_ed = allGroupMsgTotal;

                    //向前端发送用户群组列表GroupList
                    sendInfo(userId, groupObjects.toString());
                }

                //每隔一段时间检测一次消息更新
                try {
                    Thread.sleep(imConfig.getUpdateRate()*1000);
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
    public void onOpen(Session session,@PathParam(value = "token") String token, @PathParam(value = "userId") String userId) throws IOException {

        sessionPools.put(userId, session);//添加用户
        addOnlineCount();

        //如果开启了用户登录状态检测
        if(imConfig.getLoginAuth() == 1) {
            //验证用户登录状态
            if(!loginUtil.loginCheck(userId,token)){

                //验证失败，向客户端发送440状态码
                sendInfo(userId,"440");

                sessionPools.remove(userId);//删除用户
                subOnlineCount();
                session.close();//断开连接
                return;
            }
        }

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
