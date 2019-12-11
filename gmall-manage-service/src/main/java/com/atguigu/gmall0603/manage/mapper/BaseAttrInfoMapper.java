package com.atguigu.gmall0603.manage.mapper;

import com.atguigu.gmall0603.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    /**
     * 根据三级分类Id 查询平台属性集合对象 | 编写xml 文件
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> selectBaseAttrInfoListByCatalog3Id(String catalog3Id);

    /**
     * 通过平台属性值Id 查询数据
     * @param valueIds {14,81,168,171}
     * @return
     */
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param("valueIds") String valueIds);
}
