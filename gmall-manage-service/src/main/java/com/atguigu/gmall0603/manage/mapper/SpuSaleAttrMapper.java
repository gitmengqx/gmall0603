package com.atguigu.gmall0603.manage.mapper;

import com.atguigu.gmall0603.bean.SpuSaleAttr;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {
    // 根据spuId 查询销售属性集合
    List<SpuSaleAttr> selectSpuSaleAttrList(String spuId);

    // 根据spuId，skuId 查询销售属性集合
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(String skuId, String spuId);
}
