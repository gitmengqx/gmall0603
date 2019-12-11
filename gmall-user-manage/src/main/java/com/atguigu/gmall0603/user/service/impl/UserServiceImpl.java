package com.atguigu.gmall0603.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0603.bean.UserAddress;
import com.atguigu.gmall0603.bean.UserInfo;
import com.atguigu.gmall0603.config.RedisUtil;
import com.atguigu.gmall0603.service.UserService;
import com.atguigu.gmall0603.user.mapper.UserAddressMapper;
import com.atguigu.gmall0603.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserServiceImpl implements UserService{


    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24*7;


    // 调用mapper 层
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserInfo> findByUserList(UserInfo userInfo) {
        return null;
    }

    @Override
    public List<UserInfo> findLikeByLoginName(UserInfo userInfo) {
        return null;
    }

    @Override
    public List<UserInfo> findLikeByLoginName(String loginName) {
        return null;
    }

    @Override
    public void addUser(UserInfo userInfo) {

    }

    @Override
    public void delUser(UserInfo userInfo) {

    }

    @Override
    public void updUser(UserInfo userInfo) {

    }

    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        // 操作哪个数据库表，则就使用哪个表对应的mapper！
        // new Example() ; 你操作的哪个表，则对应的传入表的实体类！
        // select * from userAddress where userId = ？;
//        UserAddress userAddress = new UserAddress();
//        userAddress.setUserId(userId);
//        List<UserAddress> userAddressList = userAddressMapper.select(userAddress);
        Example example = new Example(UserAddress.class);
        // property 表示实体类的属性 
        example.createCriteria().andEqualTo("userId",userId);
        List<UserAddress> userAddressList = userAddressMapper.selectByExample(example);
        return userAddressList;
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        // select * from userInfo where userName = ? and passwd = ?
        // 注意密码是加密：
        String passwd = userInfo.getPasswd(); //123
        // 将passwd 进行加密
        String newPasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(newPasswd);
        UserInfo info = userInfoMapper.selectOne(userInfo);
        if (info!=null){
            // 获取Jedis
            Jedis jedis = redisUtil.getJedis();

            // 定义key user:userId:info
            String userKey = userKey_prefix+info.getId()+userinfoKey_suffix;

            jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(info));

            jedis.close();
            return info;
        }
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        // 从缓存获取数据
        // 必须得到Jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String userKey = userKey_prefix+userId+userinfoKey_suffix;

        String userJson = jedis.get(userKey);
        if (!StringUtils.isEmpty(userJson)){
            // userJson 转换为对象并返回去
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);

            jedis.close();
            return userInfo;
        }
        return null;
    }
}
