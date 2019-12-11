package com.atguigu.gmall0603.manage.service.impl;

// @PathVariable

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0603.bean.*;
import com.atguigu.gmall0603.config.RedisUtil;
import com.atguigu.gmall0603.manage.constant.ManageConst;
import com.atguigu.gmall0603.manage.mapper.*;
import com.atguigu.gmall0603.service.ManageService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService {
    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        // select * from baseCatalog2 where catalog1Id = ?
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        List<BaseCatalog2> baseCatalog2List = baseCatalog2Mapper.select(baseCatalog2);
        return baseCatalog2List;
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        // select * from baseCatalog3 where catalog2Id = ?
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(String catalog3Id) {
        // select * from baseAttrInfo where catalog3Id=?
//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//        baseAttrInfo.setCatalog3Id(catalog3Id);
//        return baseAttrInfoMapper.select(baseAttrInfo);
        // 调用mapper：
        return baseAttrInfoMapper.selectBaseAttrInfoListByCatalog3Id(catalog3Id);

    }

    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        // 什么情况下 是添加，什么情况下是更新，修改 根据baseAttrInfo 的Id
        // baseAttrInfo
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {
            // 修改数据
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        } else {
            // 新增
            // baseAttrInfo 插入数据
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        // baseAttrValue 平台属性值
        // 修改：通过先删除{baseAttrValue}，在新增的方式！
        // 删除条件：baseAttrValue.attrId = baseAttrInfo.id
        BaseAttrValue baseAttrValueDel = new BaseAttrValue();
        baseAttrValueDel.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueDel);

        // 获取页面传递过来的所有平台属性值数据
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList != null && attrValueList.size() > 0) {
            // 循环遍历
            for (BaseAttrValue baseAttrValue : attrValueList) {
                // 获取平台属性Id 给attrId
                baseAttrValue.setAttrId(baseAttrInfo.getId()); // ?
                baseAttrValueMapper.insertSelective(baseAttrValue);
            }
        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        // select * from baseAttrValue where attrId = ?
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);
        return baseAttrValueList;
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        // 查询到最新的平台属性值集合数据放入平台属性中！
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuInfoList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        // ctrl+u 返回接口
        return spuInfoMapper.select(spuInfo);

    }

    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
//        spuInfo 商品表
        spuInfoMapper.insertSelective(spuInfo);
//        spuImage 商品图片表
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }
//        spuSaleAttr 销售属性表
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                //        spuSaleAttrValue 销售属性值表
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        /*
            skuInfo 库存单元表 --- spuInfo！
            skuImage 库存单元图片表 --- spuImage!
            skuSaleAttrValue sku销售属性值表{sku与销售属性值的中间表} --- skuInfo ，spuSaleAttrValue
            skuAttrValue sku与平台属性值的中间表 --- skuInfo ，baseAttrValue
         */
        skuInfoMapper.insertSelective(skuInfo);
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null && skuImageList.size() > 0) {

            // 循环遍历
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        // 调用判断集合方法
        if (isNotEmptyLIst(skuSaleAttrValueList)) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (isNotEmptyLIst(skuAttrValueList)) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
//        if (skuAttrValueList!=null && skuAttrValueList.size()>0){
//            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
//                skuAttrValue.setSkuId(skuInfo.getId());
//                skuAttrValueMapper.insertSelective(skuAttrValue);
//            }
//        }
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {

        return getSkuInfoRedisson(skuId);
    }

    private SkuInfo getSkuInfoRedisson(String skuId) {
        SkuInfo skuInfo = null;
        Jedis jedis = null;
        try {
            // 获取Jedis
            jedis = redisUtil.getJedis();

            // 定义key sku:skuId:info
            String userKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;

            //  从缓存中获取数据
            String skuJson = jedis.get(userKey);
            if (StringUtils.isEmpty(skuJson)){
                // 使用redisson 的分布式锁
                Config config = new Config();
                config.useSingleServer().setAddress("redis://192.168.67.224:6379");
                // 初始化redisson
                RedissonClient redisson  = Redisson.create(config);
                // 使用redisson
                RLock lock = redisson.getLock("my-lock");
                boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
                if (res){
                    try {
                        // 从数据库中获取数据，并放入缓存！
                        // 从数据库中获取并放入缓存
                        skuInfo =  getSkuInfoDB(skuId);
                        // 使用String 类型
                        jedis.setex(userKey,ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));

                        // 添加lua 脚本。。。此处不需要使用lua 脚本！
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        // 解锁
                        lock.unlock();
                    }
                }
            }else {
                // 缓存有数据
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                return skuInfo;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (jedis!=null){
                jedis.close();
            }
        }

        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoRedisSet(String skuId) {
        SkuInfo skuInfo = null;
        Jedis jedis = null;
        try {
            // 获取Jedis
            jedis = redisUtil.getJedis();
            // 定义key sku:skuId:info
            String userKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;

            //  分布式锁
            String skuJson = jedis.get(userKey);
            if (StringUtils.isEmpty(skuJson)){
                // 说明缓存中没有数据，准备上锁，从数据库中获取数据
                // set k1 v1 px 10000 nx
                // sku:skuId:lock
                String skuLockKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKULOCK_SUFFIX;
                // 锁的值
                String token = UUID.randomUUID().toString().replace("-","");
                // 执行方法
                String lockKey   = jedis.set(skuLockKey, token, "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
                if ("OK".equals(lockKey)){
                    System.out.println("获取到了分布式锁！");
                    // 从数据库中获取并放入缓存
                    skuInfo =  getSkuInfoDB(skuId);
                    // 使用String 类型
                    jedis.setex(userKey,ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));

                    // 删除锁！
                    // jedis.del(lockKey);
                    // 使用lua 脚本来防止误删key！
                    String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    jedis.eval(script, Collections.singletonList(skuLockKey),Collections.singletonList(token));

                    return skuInfo;
                }else {
                    // 等待
                    Thread.sleep(10000);
                    return getSkuInfo(skuId);
                }
            }else {
                // 缓存有数据
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);

                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭链接
            if (jedis!=null){
                jedis.close();
            }
        }

        return getSkuInfoDB(skuId);
    }

    public SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        // 根据skuId 查询图片列表集合
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);

        skuInfo.setSkuImageList(skuImageList);

        // setSkuAttrValueList() 赋值
        // select * from skuAttrValue where skuId = ?
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValueList);
        return skuInfo;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        /*
        sql：SELECT * FROM base_attr_info bai INNER JOIN base_attr_value bav ON bai.id = bav.attr_id WHERE bav.id in (14,81,168,171);
        第一种方案：attrValueIdList 变成字符串传递到sql
        第二种方案：在xxxMapper.xml 中 使用<foreach > 动态标签库 来遍历集合得到集合中的每一条数据
        */
        String attrValueIds  = org.apache.commons.lang3.StringUtils.join(attrValueIdList.toArray(), ","); // attrValueIds = 14,81,168,171 ,....

        return baseAttrInfoMapper.selectAttrInfoListByIds(attrValueIds);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    @Override
    public Map getSkuValueIdsMap(String spuId) {
        Map<Object, Object> map = new HashMap<>();
        // key = 125|123 ,value = 37
        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
        if (mapList != null && mapList.size() > 0) {
            // 循环遍历
            for (Map skuMap : mapList) {
                // key = 125|123 ,value = 37
                map.put(skuMap.get("value_ids"), skuMap.get("sku_id"));
            }
        }
        return map;
    }

    // 泛型方法
    public <T> boolean isNotEmptyLIst(List<T> list) {
        if (list != null && list.size() > 0) {
            return true;
        }
        return false;
    }
}
