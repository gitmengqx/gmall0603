package com.atguigu.gmall0603.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuLsInfo implements Serializable {
    String id;

    BigDecimal price;

    String skuName;

    String catalog3Id;

    String skuDefaultImg;
    // 默认排序规则
    Long hotScore=0L;
    // 平台属性值
    List<SkuLsAttrValue> skuAttrValueList;

}
