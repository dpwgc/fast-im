# Fast-IM

## 基于Spring Boot + WebSocket + Redis的临时群聊系统

* 使用WebSocet进行消息广播。
* 使用Redis list存储群聊消息，key为 "gml:"+群组id，value为群组消息列表（JSON格式）。
* 使用Redis set存储用户加入的群组列表，key为 "ugs:"+用户id，value为用户当前加入的所有群组id集合。

***
## 实现功能
* 临时群聊搭建。
* 自动清除废弃的群聊。
* 实时推送用户所加入的群组列表的最新信息。
* 广播推送消息。
* 撤回消息。
***

## 使用说明

* 配置application.yml文件中的参数
* 启动项目

***

## WebSocket连接说明

### 群组聊天室连接（监听群内聊天消息更新）
>ws://127.0.0.1:9000/group/chat/{groupId}/{userId}

* 连接建立后服务端将返回该群组最新的一批消息列表list与该群组消息总数total（以JSON字符串形式推送），格式如下：
```json
{
  "list":[
    {"userId":"3","info":"1-hello","ts":1648368380132}, 
    {"userId":"1","info":"2-hello","ts":1648368386964}, 
    {"userId":"1","info":"3-hello","ts":1648368388389}, 
    {"userId":"3","info":"4-hello","ts":1648368390249}, 
    {"userId":"1","info":"5-hello","ts":1648368391742}, 
    {"userId":"2","info":"6-hello","ts":1648368393362}, 
    {"userId":"1","info":"7-hello","ts":1648368394696}, 
    {"userId":"6","info":"8-hello","ts":1648368396091}, 
    {"userId":"1","info":"9-hello","ts":1648368397434}, 
    {"userId":"1","info":"0-hello","ts":1648368400179}
  ], 
  "total":399
}
```
`userId:发送该消息的用户id`
`info:消息主体信息`
`ts:消息创建时间戳（毫秒级）`

* 在此之后客户端即可向服务端发送消息

* 客户端发送
```json
"im hello"
```

* 然后服务端向所有在线的群组成员推送该条消息（以JSON字符串形式推送），格式如下：
```json
{
  "userId":1,
  "info":"im hello",
  "ts":1648380678385
}
```

### 首页群组列表连接（监听用户加入的所有群组数据更新）
>ws://127.0.0.1:9000/group/list/{userId}

* 连接建立后服务端周期性地向客户端发送群组信息列表（以JSON字符串形式推送），客户端实时更新列表数据，推送数据格式如下：
```json
[
  {
    "newMessage":
      {
        "userId":"2",
        "info":"hi",
        "ts":1648380678385
      }, 
    "total":1
  }, 
  {
    "newMessage":
      {
        "userId":"1",
        "info":"hello",
        "ts":1648380642476
      },
    "total":17
  }
]
```
`newMessage:该群组当前最新的一条消息`
`total:该群组当前消息总数`

***
## HTTP接口文档

### 群内消息撤回

#### 接口URL
> http://127.0.0.1:9000/group/delMessage

#### 请求方式
> POST

#### Content-Type
> form-data

#### 请求Body参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
userId | 1 | Text | 是 | 用户id
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

#### 请求Body参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
userId | 1 | Text | 是 | 用户id
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

#### 请求Body参数
参数名 | 示例值 | 参数类型 | 是否必填 | 参数描述
--- | --- | --- | --- | ---
userId | 1 | Text | 是 | 用户id

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
### 返回群组消息列表

#### 接口URL
> http://127.0.0.1:9000/group/listMessage

#### 请求方式
> POST

#### Content-Type
> form-data

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
		"{\"userId\":\"1\",\"info\":\"1\",\"ts\":1648368380132}",
		"{\"userId\":\"1\",\"info\":\"2\",\"ts\":1648368386964}",
		"{\"userId\":\"1\",\"info\":\"3\",\"ts\":1648368388389}",
		"{\"userId\":\"1\",\"info\":\"4\",\"ts\":1648368390249}",
		"{\"userId\":\"1\",\"info\":\"5\",\"ts\":1648368391742}",
		"{\"userId\":\"1\",\"info\":\"6\",\"ts\":1648368393362}"
	]
}
```
***

## 项目结构

* config `配置层`
   * IMConfig `IM基础功能配置`
   * RedisConfig `Redis配置类`
   * WebSocketConfig `websocket配置类`
* controller `控制器层`
   * GroupController `群组操作接口`
   * UserController `用户操作接口`
* dao `模板层`
   * GroupObject `群组对象`
   * MessageList `消息列表封装`
   * MessageObject `消息对象`
* server `websocket服务层`
   * GroupChatServer `群组聊天室连接（监听群内聊天消息更新）`
   * GroupListServer `首页群组列表连接（监听用户加入的所有群组数据更新）`
* service `控制器服务层`
   * GroupService `群组操作服务`
   * UserService `用户操作服务`
* util `工具集合`
   * RedisUtil `Redis工具类`
   * ResultUtil `http请求返回模板`
* FastimApplication `启动类`

