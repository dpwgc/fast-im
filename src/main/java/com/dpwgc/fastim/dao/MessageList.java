package com.dpwgc.fastim.dao;

import java.util.List;

/**
 * 消息列表封装模板
 */
public class MessageList {

    //群组部分消息列表
    private List<Object> list;

    //群组消息总数
    private long total;

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTotal() {
        return total;
    }

    public void setList(List<Object> list) {
        this.list = list;
    }

    public List<Object> getList() {
        return list;
    }

    @Override
    public String toString() {
        return "{" +
                "list:" + list +
                ", total:" + total +
                '}';
    }
}
