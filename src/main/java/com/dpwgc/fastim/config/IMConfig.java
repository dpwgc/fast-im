package com.dpwgc.fastim.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * IM配置信息获取，主要是给server/ChatServer类使用
 */
@Configuration
public class IMConfig implements InitializingBean {

    //消息过期清除时限
    @Value("${im.timeout}")
    private long timeout;

    //每次请求获取的消息数量
    @Value("${im.listNum}")
    private long listNum;

    private static long TIMEOUT;
    private static long LISTNUM;

    /**
     * spring boot项目启动后自动执行
     */
    @Override
    public void afterPropertiesSet() {
        //将配置文件中的信息加载到静态变量中
        TIMEOUT = timeout;
        LISTNUM = listNum;
    }

    public long getTimeout(){
        return TIMEOUT;
    }

    public long getListNum(){
        return LISTNUM;
    }
}
