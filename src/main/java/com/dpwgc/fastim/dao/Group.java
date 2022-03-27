package com.dpwgc.fastim.dao;

public class Group {

    private Object newMessage;
    private long total;

    public void setTotal(long total) {
        this.total = total;
    }

    public void setNewMessage(Object newMessage) {
        this.newMessage = newMessage;
    }

    public long getTotal() {
        return total;
    }

    public Object getNewMessage() {
        return newMessage;
    }

    @Override
    public String toString() {
        return "{" +
                "newMessage:" + newMessage +
                ", total:" + total +
                '}';
    }
}
