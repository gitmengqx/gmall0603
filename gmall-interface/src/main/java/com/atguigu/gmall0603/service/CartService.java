package com.atguigu.gmall0603.service;

import com.atguigu.gmall0603.bean.CartInfo;

import java.util.List;

public interface CartService {
    // 添加购物车 用户Id，商品Id，商品数量。
    void  addToCart(String skuId,String userId,Integer skuNum);

    // 通过用户Id 查询购物车列表
    List<CartInfo> getCartList(String userId);


    /**
     *  合并购物车
     * @param cartInfoArrayList     未登录购物车数据
     * @param userId    通过用户Id查询登录时购物车数据
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartInfoArrayList, String userId);

    /**
     * 删除未登录购物车数据
     * @param userTempId
     */
    void deleteCartList(String userTempId);

    /**
     * 更新选中状态
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, String isChecked, String skuId);

    /**
     * 根据用户Id 查询购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据userId 查询实时价格
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
