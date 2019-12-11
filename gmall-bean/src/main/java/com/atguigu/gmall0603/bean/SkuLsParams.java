package com.atguigu.gmall0603.bean;

import lombok.Data;

import java.io.Serializable;

// 用户输入条件的参数
@Data
public class SkuLsParams implements Serializable{
    // 商品名称skuName
    String  keyword;

    String catalog3Id;

    String[] valueId;
    // 默认第一页
    int pageNo=1;

    // 每页显示的条数
    int pageSize=20;
}
