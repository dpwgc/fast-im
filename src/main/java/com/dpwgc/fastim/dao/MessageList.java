package com.dpwgc.fastim.dao;

import java.util.List;

public class MessageList {

    private List<Object> limitList;
    private long total;

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTotal() {
        return total;
    }

    public void setLimitList(List<Object> limitList) {
        this.limitList = limitList;
    }

    public List<Object> getLimitList() {
        return limitList;
    }

    @Override
    public String toString() {
        return "{" +
                "limitList:" + limitList +
                ", total:" + total +
                '}';
    }
}
