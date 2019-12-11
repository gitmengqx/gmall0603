package com.atguigu.gmall0603.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0603.bean.*;
import com.atguigu.gmall0603.config.LoginRequire;
import com.atguigu.gmall0603.service.CartService;
import com.atguigu.gmall0603.service.ManageService;
import com.atguigu.gmall0603.service.OrderService;
import com.atguigu.gmall0603.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Controller
public class OrderController {

//    @Autowired
    @Reference
    private UserService userService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;
    
    // http://localhost:8081/trade?userId=1
    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){
        // 获取到用户Id
        String userId = (String) request.getAttribute("userId");
        // return userService.findUserAddressListByUserId(userId);
        List<UserAddress> userAddressList = userService.findUserAddressListByUserId(userId);
        request.setAttribute("userAddressList",userAddressList);

        // 渲染送货清单
        // 先得到用户想要购买的商品！
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        // 声明一个集合来存储订单明细
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();
        for (CartInfo cartInfo : cartInfoList) {
            OrderDetail orderDetail = new OrderDetail();

            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());

            // 添加到集合
            detailArrayList.add(orderDetail);
        }
        // 计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        orderInfo.sumTotalAmount();
        request.setAttribute("detailArrayList",detailArrayList);
        // 保存总金额
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        
        // 获取流水号
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo",tradeNo);

        return "trade";
    }

    // http://trade.gmall.com/submitOrder
    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){

        // 获取到用户Id
        String userId = (String) request.getAttribute("userId");
        orderInfo.setUserId(userId);

        // 获取前台页面的流水号
        String tradeNo = request.getParameter("tradeNo");

        // 调用服务层的比较方法
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag){
            // 比较失败！
            request.setAttribute("errMsg","不能重复提交订单！");
            return "tradeFail";
        }

        //  删除流水号
        orderService.deleteTradeNo(userId);
        // 验证库存：
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 验证库存：
            boolean result = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
            if (!result){
                request.setAttribute("errMsg",orderDetail.getSkuName()+"库存不足！");
                return "tradeFail";
            }
            // 验证价格：
            SkuInfo skuInfoDB = manageService.getSkuInfoDB(orderDetail.getSkuId());

            if (orderDetail.getOrderPrice().compareTo(skuInfoDB.getPrice())!=0){
                request.setAttribute("errMsg",orderDetail.getSkuName()+"价格有变动！");
                // 重新查询价格！
                cartService.loadCartCache(userId);
                return "tradeFail";
            }
        }
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        // 验证通过，保存订单！
        String orderId = orderService.saveOrderInfo(orderInfo);
        // 发送延迟队列
//        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
        orderService.closeOrderInfo(orderInfo.getOutTradeNo(),10);
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }
    // http://order.gmall.com/orderSplit?orderId=xxx&wareSkuMap=xxx
    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        // [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        String wareSkuMap = request.getParameter("wareSkuMap");

        // 拆单：获取到的子订单集合
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(orderId,wareSkuMap);
        // 声明一个存储map的集合
        ArrayList<Map> mapArrayList = new ArrayList<>();
        // 生成子订单集合
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map map = orderService.initWareOrder(orderInfo);
            // 添加到集合中！
            mapArrayList.add(map);
        }
        return JSON.toJSONString(mapArrayList);
    }

}
