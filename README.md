# Fast-IM

## 基于Spring Boot + WebSocket + Redis的精简群聊系统

* 使用WebSocet进行消息广播。
* 使用Redis list存储群聊消息，key为 "gml:"+群组id，value为群组消息列表（JSON格式）。
* 使用Redis set存储用户加入的群组列表，key为 "ugs:"+用户id，value为用户当前加入的所有群组id集合。

***

## 项目结构

* config `配置层`
   * IMConfig `IM基础功能配置`
   * RedisConfig `Redis配置类`
   * WebSocketConfig `websocket配置类`
* controller `控制器层`
   * MessageController `消息操作接口`
   * UserController `用户操作接口`
* dao `模板层`
   * GroupList `群组列表封装`
   * GroupObject `群组对象`
   * MessageList `消息列表封装`
   * MessageObject `消息对象`
* server `websocket服务层`
   * GroupChatServer `群组聊天室连接（监听群内聊天消息更新）`
   * GroupListServer `首页群组列表连接（监听用户加入的所有群组数据更新）`
* service `控制器服务层`
   * MessageService `消息操作服务`
   * UserService `用户操作服务`
* util `工具集合`
   * RedisUtil `Redis工具类`
   * ResultUtil `http请求返回模板`
* FastimApplication `启动类`

***

## 使用说明

* 配置application.yml文件中的参数
* 启动项目

