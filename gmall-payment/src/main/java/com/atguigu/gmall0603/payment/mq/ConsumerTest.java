package com.atguigu.gmall0603.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

// 发送消息
public class ConsumerTest {

    public static void main(String[] args) throws JMSException {
        /*
        1.  获取消息工厂
        2.  打开连接
        3.  创建session
        4.  创建队列，消息消费者
        5.  创建消息对象
        6.  发送消息
        7.  关闭
         */
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.67.224:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
        // Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        Queue atguigu = session.createQueue("test-atguigu");

        MessageConsumer consumer = session.createConsumer(atguigu);

        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                // 消费消息
                if (message instanceof TextMessage){
                    String text = null;
                    try {
                        text = ((TextMessage) message).getText();
                        message.acknowledge();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                    System.out.println(text);
                }

            }
        });


    }
}
