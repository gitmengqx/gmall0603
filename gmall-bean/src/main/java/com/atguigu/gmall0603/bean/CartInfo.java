package com.atguigu.gmall0603.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CartInfo implements Serializable{
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column
    String id;
    @Column
    String userId;
    @Column
    String skuId;
    // 添加时的价格
    @Column
    BigDecimal cartPrice;
    @Column
    Integer skuNum;
    @Column
    String imgUrl;
    @Column
    String skuName;
    // 1 表示默认选中
    @Column
    String isChecked="1";

    // 实时价格 skuInfo.price
    @Transient
    BigDecimal skuPrice;

}
