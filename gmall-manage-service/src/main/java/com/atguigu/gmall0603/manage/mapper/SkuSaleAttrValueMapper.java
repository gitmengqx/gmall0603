package com.atguigu.gmall0603.manage.mapper;

import com.atguigu.gmall0603.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    // 根据spuId 查询数据
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);

    // 根据spuId 查询map 集合数据
    List<Map> getSaleAttrValuesBySpu(String spuId);
}
