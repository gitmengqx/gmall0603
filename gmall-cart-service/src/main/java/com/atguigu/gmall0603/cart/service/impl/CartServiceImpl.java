package com.atguigu.gmall0603.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0603.bean.CartInfo;
import com.atguigu.gmall0603.bean.SkuInfo;
import com.atguigu.gmall0603.cart.constant.CartConst;
import com.atguigu.gmall0603.cart.service.mapper.CartInfoMapper;
import com.atguigu.gmall0603.config.RedisUtil;
import com.atguigu.gmall0603.service.CartService;
import com.atguigu.gmall0603.service.ManageService;
import org.apache.commons.codec.language.Nysiis;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;
    
    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        /*
        0.  数据存储mysql --- redis
        1.  当购物车中没用该商品的时候，则直接添加到购物车！insert
        2.  如果购物车中有该商品，则商品数量 相加！update
        3.  必须更新缓存！
         */
        // 获取Jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key user:userId:cart
        String cartKey= CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        // 判断缓存中是否有cartKey，先加载数据库中的数据放入缓存！
        if (!jedis.exists(cartKey)){
            loadCartCache(userId);
        }

        // 确定数据类型？
        /*
            redis 存储数据 采用hash
            hset(key,field,value)
            key = user:userId:cart
            field = skuId
            value = cartInfo 的字符串
         */
        // select * from cartInfo where userId = ? and skuId = ?
       /* CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        cartInfo.setSkuId(skuId);
        CartInfo cartInfoExist  = cartInfoMapper.selectOne(cartInfo);*/
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userId).andEqualTo("skuId",skuId);
        CartInfo cartInfoExist = cartInfoMapper.selectOneByExample(example);

        // 说明购物车中有该商品
        if (cartInfoExist!=null){
            //  如果购物车中有该商品，则商品数量 相加！update
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            // 初始化实时价格
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            // 更新数据库
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
            // 更新缓存
            // jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));
        }else {
            // 当购物车中没用该商品的时候，则直接添加到购物车！insert
            CartInfo cartInfo1 = new CartInfo();
            // 购物车数据是从商品详情得到 {skuInfo}
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setSkuId(skuId);
            cartInfo1.setUserId(userId);
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            // 添加数据库
            cartInfoMapper.insertSelective(cartInfo1);
            cartInfoExist=cartInfo1;
            // 更新缓存
            // jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfo1));

        }
        // 更新缓存
        jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));
        // 设置过期时间
        setCartKeyExpire(userId, jedis, cartKey);
        // 关闭缓存
        jedis.close();

    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        // 什么一个返回的集合对象
        List<CartInfo> cartInfoList = new ArrayList<>();
        /*
        1.  根据用户Id 查询 {先查询缓存，缓存没有，再查询数据库}
         */
        // 获取Jedis
        Jedis jedis = redisUtil.getJedis();

        // 定义key user:userId:cart
        String cartKey= CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        // 获取数据
        //        jedis.hget() 不能使用！
        //  jedis.hgetAll(); skuId , value
        List<String> stringList = jedis.hvals(cartKey);
        if (stringList!=null && stringList.size()>0){
            for (String cartStr : stringList) {
                // 将从redis 中获取的数据添加到集合
                cartInfoList.add(JSON.parseObject(cartStr,CartInfo.class));
            }

            // 购物车列表显示有顺序：按照商品的更新时间 降序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    // str1 = ab str2 = ac;
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else {
            // 缓存中没用数据！
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartInfoArrayList, String userId) {
        // 获取到登录时购物车数据
        List<CartInfo> cartInfoListLogin = cartInfoMapper.selectCartListWithCurPrice(userId);
        // 判断登录时购物车数据是否为空？
        //        if (cartInfoListLoginDB!=null && cartInfoListLoginDB.size()>0){
        //
        //        }
        /*
        demo1:
            登录：
                37	1
                38	1
            未登录：
                37	1
                38	1
                39	1
            合并之后的数据
                37	2
                38	2
                39	1
         demo2:
             未登录：
                37	1
                38	1
                39	1
                40  1
              合并之后的数据
                37	1
                38	1
                39	1
                40  1
         */
        if (cartInfoListLogin!=null && cartInfoListLogin.size()>0){
            for (CartInfo cartInfoNoLogin : cartInfoArrayList) {
                // 声明一个boolean 类型变量。
                boolean isMatch = false;
                // 如果说数据库中一条数据都没有？
                for (CartInfo cartInfoLogin : cartInfoListLogin) {
                    // 操作 37 38 可能会发生异常？
                    if (cartInfoNoLogin.getSkuId().equals(cartInfoLogin.getSkuId())){
                        // 数量相加
                        cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum());
                        // 更新数据库
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfoLogin);
                        isMatch=true;
                    }
                }
                // 表示登录的购物车数据与未登录购物车数据没用匹配上！ 39	1
                if (!isMatch){
                    //  直接添加到数据库
                    cartInfoNoLogin.setId(null);
                    cartInfoNoLogin.setUserId(userId);
                    cartInfoMapper.insertSelective(cartInfoNoLogin);
                }
            }
        }else {
            // 数据库为空！直接添加到数据库！
            for (CartInfo cartInfo : cartInfoArrayList) {
                cartInfo.setId(null);
                cartInfo.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfo);
            }
        }
        // 汇总数据 37 38 39
        List<CartInfo> cartInfoList = loadCartCache(userId); // 数据库中的数据
        // 合并数据：勾选
        for (CartInfo cartInfo : cartInfoList) { // 登录
            for (CartInfo info : cartInfoArrayList) { // 未登录
                // 合并的条件
                if (cartInfo.getSkuId().equals(info.getSkuId())){
                    // 未登录状态选中的商品！
                    if ("1".equals(info.getIsChecked())){
                        cartInfo.setIsChecked("1");
                        // 自动勾选
                        checkCart(userId,"1",info.getSkuId());
                    }
                }
            }
        }
        return cartInfoList;
    }
    @Override
    public void deleteCartList(String userTempId) {
        // 删除数据库 再删除缓存
        // delete from userInfo where userId = ?userTempId
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userTempId);
        cartInfoMapper.deleteByExample(example);

        // 删除缓存
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String cartKey = CartConst.USER_KEY_PREFIX+userTempId+CartConst.USER_CART_KEY_SUFFIX;

        jedis.del(cartKey);

        jedis.close();

        // 临时userTempId {删除}

    }

    @Override
    public void checkCart(String userId, String isChecked, String skuId) {
        // update cartInfo set isChecked=? where  skuId = ? and userId=？
        // 修改数据库
        // 第一个参数表示修改的数据，第二个参数表示条件
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("skuId",skuId).andEqualTo("userId",userId);
        cartInfoMapper.updateByExampleSelective(cartInfo,example);
        // 修改缓存
        // 获取Jedis
        Jedis jedis = redisUtil.getJedis();

        // 定义key user:userId:cart
        String cartKey= CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 先获取用户选择的商品
        String cartJson = jedis.hget(cartKey, skuId);

        // cartJson 转换为对象
        CartInfo cartInfoUpd = JSON.parseObject(cartJson, CartInfo.class);
        cartInfoUpd.setIsChecked(isChecked);
        // cartInfoUpd 写会缓存
        jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfoUpd));
        // 关闭
        jedis.close();

        // 对于缓存与数据库进行数据同步时： 如果对数据库进行了DML 操作。则应该 先upd ，再删除缓存！

    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 被选中的购物车：
        // 获取Jedis
        Jedis jedis = redisUtil.getJedis();

        // 定义key user:userId:cart
        String cartKey= CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        List<String> stringList = jedis.hvals(cartKey);

        if (stringList!= null && stringList.size()>0){
            for (String cartJson : stringList) {
                // 获取选中的商品！
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                if (cartInfo.getIsChecked().equals("1")){
                    cartInfoList.add(cartInfo);
                }
            }
        }
        // 如果缓存没有，则走数据库！
        jedis.close();
        return cartInfoList;
    }

    // 通过userId 查询购物车并放入缓存！
    public List<CartInfo> loadCartCache(String userId) {
        // 获取Jedis
        Jedis jedis = redisUtil.getJedis();

        // 定义key user:userId:cart
        String cartKey= CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        // 查询实时价格：cartInfo.setSkuprice(skuInfo.getPrice());
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList==null || cartInfoList.size()==0){
            return  null;
        }
        // 将数据库中的数据查询并放入缓存
//        for (CartInfo cartInfo : cartInfoList) {
//            jedis.hset(cartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
//        }
        HashMap<String, String> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        jedis.hmset(cartKey,map);
        // 关闭jedis
        jedis.close();
        return cartInfoList;

    }

    // 设置过期时间
    private void setCartKeyExpire(String userId, Jedis jedis, String cartKey) {
        // 设置过期时间 | 计算用户的购买力！|
        // 购物车过期时间可以根据用户的过期时间来设置
        // 获取用户的key
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        if (jedis.exists(userKey)){
            // 获取key 的过期时间
            Long ttl = jedis.ttl(userKey);
            // 说明登录
            jedis.expire(cartKey,ttl.intValue());
        }else {
            // 未登录
            jedis.expire(cartKey,7*24*3600);
        }
    }
}
