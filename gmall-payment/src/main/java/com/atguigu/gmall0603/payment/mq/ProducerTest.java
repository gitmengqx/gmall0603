package com.atguigu.gmall0603.payment.mq;

import com.atguigu.gmall0603.config.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jms.*;

// 发送消息
public class ProducerTest {


    public static void main(String[] args) throws JMSException {
        /*
        1.  获取消息工厂
        2.  打开连接
        3.  创建session
        4.  创建队列，消息提供者
        5.  创建消息对象
        6.  发送消息
        7.  关闭
         */
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.67.224:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue atguigu = session.createQueue("test-atguigu");

        MessageProducer producer = session.createProducer(atguigu);
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("上课了！");

        producer.send(activeMQTextMessage);

        // 必须提交
        //session.commit();

        producer.close();
        session.close();
        connection.close();


    }
}
