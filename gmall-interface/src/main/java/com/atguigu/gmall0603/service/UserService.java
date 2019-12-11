package com.atguigu.gmall0603.service;

import com.atguigu.gmall0603.bean.UserAddress;
import com.atguigu.gmall0603.bean.UserInfo;

import java.util.List;

public interface UserService {

    /**
     * 查询所有数据
     * @return
     */
    List<UserInfo> findAll();

    /**
     *
     * @param userInfo
     * @return
     */
    List<UserInfo> findByUserList(UserInfo userInfo);

    /**
     * like
     * @param userInfo
     * @return
     */
    List<UserInfo> findLikeByLoginName(UserInfo userInfo);

    /**
     * like
     * @param loginName
     * @return
     */
    List<UserInfo> findLikeByLoginName(String loginName);

    /**
     * 插入数据
     * @param userInfo
     */
    void addUser(UserInfo userInfo);

    /**
     *
     * @param userInfo
     */
    void delUser(UserInfo userInfo);

    /**
     *
     * @param userInfo
     */
    void updUser(UserInfo userInfo);


    /**
     * 根据用户Id 查询用户的收货地址列表！
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(String userId);


    /**
     * 登录方法
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 通过用户Id 认证是否登录！
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
