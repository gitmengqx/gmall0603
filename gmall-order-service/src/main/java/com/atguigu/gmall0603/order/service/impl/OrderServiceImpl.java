package com.atguigu.gmall0603.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0603.bean.OrderDetail;
import com.atguigu.gmall0603.bean.OrderInfo;
import com.atguigu.gmall0603.bean.enums.OrderStatus;
import com.atguigu.gmall0603.bean.enums.ProcessStatus;
import com.atguigu.gmall0603.config.ActiveMQUtil;
import com.atguigu.gmall0603.config.RedisUtil;
import com.atguigu.gmall0603.order.mapper.OrderDetailMapper;
import com.atguigu.gmall0603.order.mapper.OrderInfoMapper;
import com.atguigu.gmall0603.service.OrderService;
import com.atguigu.gmall0603.service.PaymentService;
import com.atguigu.gmall0603.util.HttpClientUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService{


    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;


    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private PaymentService paymentService;

    @Override
    @Transactional
    public String saveOrderInfo(OrderInfo orderInfo) {
        // orderInfo
        // 总金额，订单状态，用户Id，第三方交易编号，创建时间，过期时间，进程状态
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID);

        orderInfo.setCreateTime(new Date());
        // 定义为1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());

        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfoMapper.insertSelective(orderInfo);

        // 保存订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setId(null);
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        // 返回
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        // 获取Jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String tradeNoKey = "user:"+userId+":tradeCode";
        // 定义一个流水号
        String tradeNo = UUID.randomUUID().toString().replace("-","");
        jedis.set(tradeNoKey,tradeNo);

        // 关闭jedis
        jedis.close();
        return tradeNo;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        // 获取Jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String tradeNoKey = "user:"+userId+":tradeCode";

        String redisTradeNo = jedis.get(tradeNoKey);

        jedis.close();
        return tradeCodeNo.equals(redisTradeNo);
    }

    @Override
    public void deleteTradeNo(String userId) {
        // 获取Jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String tradeNoKey = "user:"+userId+":tradeCode";
        // 删除数据
        jedis.del(tradeNoKey);
        // 关闭
        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        // 远程调用http://www.gware.com /hasStock?skuId=10221&num=2
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);


        return "1".equals(result);
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);

        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        // 创建连接
        Connection connection = activeMQUtil.getConnection();
        // wareJson 要传入的json字符串
        String wareJson=initWareOrder(orderId);
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            // 创建消息提供者
            MessageProducer producer = session.createProducer(order_result_queue);
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            activeMQTextMessage.setText(wareJson);
            producer.send(activeMQTextMessage);

            // 提交
            session.commit();

            // 关闭
            producer.close();
            session.close();
            connection.close();


        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<OrderInfo> getExpiredOrderList() {
        // 支付状态 = UNPID AND 过期时间 < 当前系统时间
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andEqualTo("processStatus",ProcessStatus.UNPAID).andLessThan("expireTime",new Date());
        List<OrderInfo> orderInfoList = orderInfoMapper.selectByExample(example);
        return orderInfoList;
    }

    @Override
//    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        // orderInfo
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
        // paymentInfo
        paymentService.closePayment(orderInfo.getId());
    }

    // 根据orderId 获取json 字符串
    private String initWareOrder(String orderId) {
        // 通过orderId 获取orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);

        // 将orderInfo中部分数据转换为Map
        Map map = initWareOrder(orderInfo);

        return JSON.toJSONString(map);
    }

    //  将orderInfo中部分数据转换为Map
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee",orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","冬天买大衣");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        /*
        details:[{skuId:101,skuNum:1,skuName:
        ’小米手64G’},
        {skuId:201,skuNum:1,skuName:’索尼耳机’}]
         */
        ArrayList<Map> mapArrayList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId",orderDetail.getSkuId());
            orderDetailMap.put("skuNum",orderDetail.getSkuNum());
            orderDetailMap.put("skuName",orderDetail.getSkuName());
            mapArrayList.add(orderDetailMap);
        }
        map.put("details",mapArrayList);
        return map;
    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        ArrayList<OrderInfo> orderInfoArrayList = new ArrayList<>();
        /*
        1.  先获取到原始订单 107
        2.  将wareSkuMap 转换为我们能操作的对象 [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
            方案一：class Param{
                        private String wareId;
                        private List<String> skuIds;
                    }
            方案二：看做一个Map mpa.put("wareId",value); map.put("skuIds",value)

        3.  创建一个新的子订单 108 109 。。。
        4.  给子订单赋值
        5.  保存子订单到数据库
        6.  修改原始订单的状态
        7.  测试
         */
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        if (maps!=null){
            for (Map map : maps) {
                String wareId = (String) map.get("wareId");

                List<String> skuIds = (List<String>) map.get("skuIds");

                OrderInfo subOrderInfo = new OrderInfo();
                // 属性拷贝
                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                // 防止主键冲突
                subOrderInfo.setId(null);
                subOrderInfo.setParentOrderId(orderId);
                // 赋值仓库Id
                subOrderInfo.setWareId(wareId);

                // 计算子订单的金额: 必须有订单明细
                // 获取到子订单明细
                // 声明一个集合来存储子订单明细
                ArrayList<OrderDetail> orderDetails = new ArrayList<>();

                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                // 表示主主订单明细中获取到子订单的明细
                if (orderDetailList!=null&&orderDetailList.size()>0){
                    for (OrderDetail orderDetail : orderDetailList) {
                        // 获取子订单明细的商品Id
                        for (String skuId : skuIds) {
                            if (skuId.equals(orderDetail.getSkuId())){
                                // 将订单明细添加到集合
                                orderDetails.add(orderDetail);
                            }
                        }
                    }
                }
                subOrderInfo.setOrderDetailList(orderDetails);
                // 计算总金额
                subOrderInfo.sumTotalAmount();
                // 保存子订单
                saveOrderInfo(subOrderInfo);
                // 将子订单添加到集合中！
                orderInfoArrayList.add(subOrderInfo);
            }
        }
        // 修改原始订单的状态
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        return orderInfoArrayList;
    }

    @Override
    public void closeOrderInfo(String outTradeNo, int delaySec) {
        // 创建工厂
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建对象
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            // 创建消息提供者
            MessageProducer producer = session.createProducer(payment_result_check_queue);
            // 创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo",outTradeNo);

            // 开启延迟队列的参数设置
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec*1000);
            // 发送消息
            producer.send(activeMQMapMessage);

            // 提交
            session.commit();
            // 关闭
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public OrderInfo getOrderInfo(OrderInfo orderInfo) {

        return orderInfoMapper.selectOne(orderInfo);
    }

    @Override
    public boolean checkPayment(OrderInfo orderInfo) {
        return false;
    }


}
