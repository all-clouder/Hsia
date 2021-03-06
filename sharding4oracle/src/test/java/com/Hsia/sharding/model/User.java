package com.Hsia.sharding.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @Auther: yumazhe
 * @Date: 2019/4/2 13:44
 * @Description:
 */
public class User implements Serializable {
    private int id;
    private int money;
    private int nodeId;
    private Date createTime;

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public User(int id, int money,int nodeId) {
        this.id = id;
        this.money = money;
        this.nodeId = nodeId;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", money=" + money +
                ", nodeId=" + nodeId +
                ", createTime=" + createTime +
                '}';
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }
}
