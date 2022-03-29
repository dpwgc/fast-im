package com.dpwgc.fastim.dao;

/**
 * 消息对象模板
 */
public class MessageObject {

    private String groupId; //该消息所属群组id
    private String userId;  //发送该消息的用户id
    private String info;    //消息主体信息
    private Long ts;        //消息创建时间戳（毫秒级）

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void setTs(Long ts) {
        this.ts = ts;
    }

    public Long getTs() {
        return ts;
    }

    public String getInfo() {
        return info;
    }

    public String getUserId() {
        return userId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }
}
