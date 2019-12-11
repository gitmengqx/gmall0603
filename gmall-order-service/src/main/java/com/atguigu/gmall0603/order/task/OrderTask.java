package com.atguigu.gmall0603.order.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0603.bean.OrderInfo;
import com.atguigu.gmall0603.service.OrderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@EnableScheduling
@Component
public class OrderTask {

    @Reference
    private OrderService orderService;

    // cron 定义轮询的规则 分 时 日 月 周 年
    // 表示每分钟的第五秒开始执行下面方法
    @Scheduled(cron = "5 * * * * ?")
    public void testOne (){
        System.out.println(Thread.currentThread().getName()+"testOne");
    }

    // 表示每隔五秒开始执行下面方法
    @Scheduled(cron = "0/5 * * * * ?")
    public void testTwo (){
        System.out.println(Thread.currentThread().getName()+"testTwo");
    }

    @Scheduled(cron = "0/20 * * * * ?")
    public void checkOrder (){
        System.out.println("开始处理过期订单");
        long starttime = System.currentTimeMillis();
        // 获取过期订单 支付状态 = UNPID AND 过期时间 < 当前系统时间
        List<OrderInfo> orderInfoList = orderService.getExpiredOrderList();
        // 如何处理过期订单
        for (OrderInfo orderInfo : orderInfoList) {
            // 更新订单的状态！
            orderService.execExpiredOrder(orderInfo);
        }
        long costtime = System.currentTimeMillis() - starttime;
        System.out.println("一共处理"+orderInfoList.size()+"个订单 共消耗"+costtime+"毫秒");
    }
}
