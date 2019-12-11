package com.atguigu.gmall0603.cart.service.mapper;

import com.atguigu.gmall0603.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CartInfoMapper extends Mapper<CartInfo> {

    /**
     * 根据userId 查询购物车集合
     * @param userId
     * @return
     */
    List<CartInfo> selectCartListWithCurPrice(String userId);
}
