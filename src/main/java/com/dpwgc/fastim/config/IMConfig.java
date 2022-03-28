package com.dpwgc.fastim.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * IM配置信息获取，主要是给server/GroupChatServer类使用（因为websocket类中无法使用@value注解接收配置参数）
 */
@Configuration
public class IMConfig implements InitializingBean {

    //群组消息列表过期清除时限
    @Value("${im.groupMessage.timeout}")
    private long timeout;

    //每次请求获取的消息数量
    @Value("${im.groupMessage.listNum}")
    private long listNum;

    //群组列表刷新频率
    @Value("${im.groupList.updateRate}")
    private int updateRate;

    //连接建立后是否自动加入群组
    @Value("${im.group.autoJoin}")
    private int autoJoin;

    //连接建立后是否校验用户登录状态
    @Value("${im.group.loginAuth}")
    private int loginAuth;

    private static long TIMEOUT;
    private static long LISTNUM;
    private static int UPDATERATE;
    private static int AUTOJOIN;
    private static int LOGINAUTH;

    /**
     * spring boot项目启动后自动执行
     */
    @Override
    public void afterPropertiesSet() {
        //将配置文件中的信息加载到静态变量中
        TIMEOUT = timeout;
        LISTNUM = listNum;
        UPDATERATE = updateRate;
        AUTOJOIN = autoJoin;
        LOGINAUTH = loginAuth;
    }

    public long getTimeout(){
        return TIMEOUT;
    }

    public long getListNum(){
        return LISTNUM;
    }

    public int getUpdateRate() {
        return UPDATERATE;
    }

    public int getAutoJoin() {
        return AUTOJOIN;
    }

    public int getLoginAuth() {
        return LOGINAUTH;
    }
}
