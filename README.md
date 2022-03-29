# Fast-IM

## 基于Spring Boot + WebSocket + Redis的分布式即时通讯群聊系统

* 使用WebSocet连接IM服务端与客户端。
* 使用Redis string存储用户登录令牌，key为"login:"+用户id，value为token。
* 使用Redis list存储群聊消息，key为 "gml:"+群组id，value为群组消息列表（JSON格式）。
* 使用Redis set存储用户加入的群组列表，key为 "ugs:"+用户id，value为用户当前加入的所有群组id集合。
* 使用Redis pub/sub订阅发布功能实现分布式WebSocket推送服务，订阅发布主题管道名称为 "mq:"+群组id（每个群组单独共享一个主题）。

***

## 实现功能
* 分布式WebSocket推送服务（基于Redis订阅/发布功能及WebSocket连接实现）。
* 临时群聊快速搭建（适用于直播间聊天、游戏内聊天）。
* 群聊历史聊天记录查询（HTTP接口实现）。
* 自动清除长期废弃的群聊（基于Redis键值过期功能）。
* 实时推送用户所加入的群组列表的最新动态（WebSocket连接实现）。
* 用户登录状态验证（Redis Token）。
* 一定时间内的消息撤回功能（HTTP接口实现）。

***

## 使用说明
* 部署Redis。
* 配置application.yml文件中的参数。
* 启动项目。
* 启动后访问 http://127.0.0.1:9000/test/websocket ，测试WebSocket连接。
***

## 自定义业务逻辑
* 可在controller/UserController及service/UserService文件中自定义用户登录逻辑。
* 可在config/InterceptorConfig及interceptor/ApiInterceptor文件中自定义http接口拦截器及拦截路由。

***

## 消息模板说明
```
groupId:消息所属群组id
userId:发送该条消息的用户id
info:消息主体内容
ts:消息创建时间戳（毫秒级）
```
为了省内存，没有消息唯一id/uuid，查询某条消息时，按照groupId、userId、ts这三个字段来匹配消息。先根据groupId查找到指定Redis list，再根据userId和ts查找到list中的指定消息（ps：即使有人在同一毫秒内向某群组插入了两条消息，也无大碍，只会略微影响消息撤回功能及获取历史消息记录功能）。
***

## WebSocket连接说明

### 群组聊天室连接（监听群内聊天消息更新）
>ws://127.0.0.1:9000/group/chat/{groupId}/{userId}/{token}
* `groupId:群组id`
* `userId:用户id`
* `token:登录令牌，默认不开启websocket令牌验证，可随意填写一串字符（不能为空）`

连接建立后服务端将返回该群组最新的一批消息列表list与该群组消息总数total（以JSON字符串形式推送），格式如下：
```json
{
  "list":[
    {"groupId":"1","userId":"3","info":"1-hello","ts":1648368380132}, 
    {"groupId":"1","userId":"1","info":"2-hello","ts":1648368386964}, 
    {"groupId":"1","userId":"1","info":"3-hello","ts":1648368388389}, 
    {"groupId":"1","userId":"3","info":"4-hello","ts":1648368390249}, 
    {"groupId":"1","userId":"1","info":"5-hello","ts":1648368391742}, 
    {"groupId":"1","userId":"2","info":"6-hello","ts":1648368393362}, 
    {"groupId":"1","userId":"1","info":"7-hello","ts":1648368394696}, 
    {"groupId":"1","userId":"6","info":"8-hello","ts":1648368396091}, 
    {"groupId":"1","userId":"1","info":"9-hello","ts":1648368397434}, 
    {"groupId":"1","userId":"1","info":"0-hello","ts":1648368400179}
  ], 
  "total":399
}
```
* `groupId:该消息所属群组id`
* `userId:发送该消息的用户id`
* `info:消息主体信息`
* `ts:消息创建时间戳（毫秒级）`

注：如果开启了websocket令牌验证，且用户登录令牌token验证失败，则服务端返回"440"代码，并断开连接。
```json
"440"
```

连接成功后客户端即可向服务端发送消息

* 客户端发送
```json
"im hello"
```

然后服务端向所有在线的群组成员推送该条消息（以JSON字符串形式推送），格式如下：
```json
{
  "groupId":"1",
  "userId":1,
  "info":"im hello",
  "ts":1648380678385
}
```

### 首页群组列表连接（监听用户加入的所有群组数据更新）
>ws://127.0.0.1:9000/group/list/{userId}/{token}
* `userId:用户id`
* `token:登录令牌，默认不开启websocket令牌验证，可随意填写一串字符（不能为空）`

连接建立后，服务端将周期性检查用户群组列表中是否有新消息到达，如果有新消息到达，则向客户端发送最新的群组信息列表（以JSON字符串形式推送），推送数据格式如下：
```json
[
  {
    "newMessage":
      {
        "groupId":"1",
        "userId":"2",
        "info":"hi",
        "ts":1648380678385
      }, 
    "total":1
  }, 
  {
    "newMessage":
      {
        "groupId":"1",
        "userId":"1",
        "info":"hello",
        "ts":1648380642476
      },
    "total":17
  }
]
```
* `newMessage:该群组当前最新的一条消息`
* `total:该群组当前消息总数`

注：如果开启了websocket令牌验证，且用户登录令牌token验证失败，则服务端返回"440"代码，并断开连接。
```json
"440"
```

***
## HTTP接口文档

### 用户登录（根据业务自定义，默认直接通过验证并返回token）

#### 接口URL
> http://127.0.0.1:9000/user/login

#### 请求方式
> POST

#### Content-Type
> form-data

#### 请求Body参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
userId | 1 | Text | 是 | 用户id
password | 123456 | Text | 是 | 密码

#### 成功响应示例
```json
{
	"code": 200,
	"msg": "8f2c1eb2099049eab5cad6a78a1f8285",
	"data": "user_info"
}
```

### 返回群组消息列表

#### 接口URL
> http://127.0.0.1:9000/group/listMessage

#### 请求方式
> POST

#### Content-Type
> form-data

#### Header参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
userId | 1 | Text | 是 | 用户id
token | 84e9d36e4c7c44e0a79bb71f1b4ce9c4 | Text | 是 | 登录令牌

#### 请求Body参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
groupId | 1 | Text | 是 | 群组id
startPage | 0 | Text | 是 | 起始页
endPage | 5 | Text | 是 | 终止页

#### 成功响应示例
```json
{
	"code": 200,
	"msg": "操作成功",
	"data": [
		"{\"groupId\":\"1\",\"userId\":\"1\",\"info\":\"1\",\"ts\":1648368380132}",
		"{\"groupId\":\"1\",\"userId\":\"1\",\"info\":\"2\",\"ts\":1648368386964}",
		"{\"groupId\":\"1\",\"userId\":\"1\",\"info\":\"3\",\"ts\":1648368388389}",
		"{\"groupId\":\"1\",\"userId\":\"1\",\"info\":\"4\",\"ts\":1648368390249}",
		"{\"groupId\":\"1\",\"userId\":\"1\",\"info\":\"5\",\"ts\":1648368391742}",
		"{\"groupId\":\"1\",\"userId\":\"1\",\"info\":\"6\",\"ts\":1648368393362}"
	]
}
```

### 返回指定消息之前的历史消息列表

#### 接口URL
> http://127.0.0.1:9000/group/listMessage

#### 请求方式
> POST

#### Content-Type
> form-data

#### Header参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
userId | 1 | Text | 是 | 用户id
token | 84e9d36e4c7c44e0a79bb71f1b4ce9c4 | Text | 是 | 登录令牌

#### 请求Body参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
userId | 1 | Text | 是 | 该消息的用户id
groupId | 1 | Text | 是 | 该消息所属群组id
ts | 1648443134344 | Text | 是 | 该消息的创建时间戳
count | 5 | Text | 是 | 要获取的消息数量

#### 成功响应示例
```json
{
	"code": 200,
	"msg": "操作成功",
	"data": [
		"{\"groupId\":\"1\",\"userId\":\"1\",\"info\":\"1\",\"ts\":1648368380132}",
		"{\"groupId\":\"1\",\"userId\":\"1\",\"info\":\"2\",\"ts\":1648368386964}",
		"{\"groupId\":\"1\",\"userId\":\"1\",\"info\":\"3\",\"ts\":1648368388389}",
		"{\"groupId\":\"1\",\"userId\":\"1\",\"info\":\"4\",\"ts\":1648368390249}"
	]
}
```

### 用户撤回自己的群聊消息

#### 接口URL
> http://127.0.0.1:9000/group/delMessage

#### 请求方式
> POST

#### Content-Type
> form-data

#### Header参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
userId | 1 | Text | 是 | 用户id
token | 84e9d36e4c7c44e0a79bb71f1b4ce9c4 | Text | 是 | 登录令牌

#### 请求Body参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
groupId | 1 | Text | 是 | 群组id
ts | 1648443134344 | Text | 是 | 消息创建时间戳

#### 成功响应示例
```json
{
	"code": 200,
	"msg": "撤回消息成功",
	"data": null
}
```
### 用户加入群聊

#### 接口URL
> http://127.0.0.1:9000/user/joinGroup

#### 请求方式
> POST

#### Content-Type
> form-data

#### Header参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
userId | 1 | Text | 是 | 用户id
token | 84e9d36e4c7c44e0a79bb71f1b4ce9c4 | Text | 是 | 登录令牌

#### 请求Body参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
groupId | 3 | Text | 是 | 群组id

#### 成功响应示例
```json
{
	"code": 200,
	"msg": "操作成功",
	"data": null
}
```
### 获取用户加入的群组列表

#### 接口URL
> http://127.0.0.1:9000/user/listGroup

#### 请求方式
> POST

#### Content-Type
> form-data

#### Header参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
userId | 1 | Text | 是 | 用户id
token | 84e9d36e4c7c44e0a79bb71f1b4ce9c4 | Text | 是 | 登录令牌

#### 成功响应示例
```json
{
	"code": 200,
	"msg": "操作成功",
	"data": [
		"3",
		"2",
		"1"
	]
}
```
***

## 项目结构

* config `配置层`
   * IMConfig `IM基础功能配置`
   * InterceptorConfig `接口拦截器配置`
   * RedisConfig `Redis配置类`
   * WebSocketConfig `websocket配置类`
* controller `控制器层`
   * GroupController `群组操作接口`
   * UserController `用户操作接口`
* dao `模板层`
   * GroupObject `群组对象`
   * MessageList `消息列表封装`
   * MessageObject `消息对象`
* interceptor `AOP拦截器`
   * ApiInterceptor `接口拦截器`
* server `websocket服务层`
   * GroupChatServer `群组聊天室连接（监听群内聊天消息更新）`
   * GroupListServer `首页群组列表连接（监听用户加入的所有群组数据更新）`
   * RedisListenServer `Redis订阅监听服务（监听所有IM服务器接收到的消息）`
* service `控制器服务层`
   * GroupService `群组操作服务`
   * UserService `用户操作服务`
* util `工具集合`
   * LoginUtil `登录验证工具`
   * RedisUtil `Redis工具类`
   * ResultUtil `http请求返回模板`
* FastimApplication `启动类`

