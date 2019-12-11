package com.atguigu.gmall0603.payment;

import com.atguigu.gmall0603.config.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPaymentApplicationTests {

	@Autowired
	private ActiveMQUtil activeMQUtil;

	@Test
	public void contextLoads() {
	}

	@Test
	public void testMsg() throws JMSException {

		  /*
        1.  获取消息工厂
        2.  打开连接
        3.  创建session
        4.  创建队列，消息提供者
        5.  创建消息对象
        6.  发送消息
        7.  关闭
         */
//		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.67.224:61616");
//		Connection connection = activeMQConnectionFactory.createConnection();
		Connection connection = activeMQUtil.getConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
		Queue atguigu = session.createQueue("test-atguigu-tools");

		MessageProducer producer = session.createProducer(atguigu);
		ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
		activeMQTextMessage.setText("没毛病！");

		producer.send(activeMQTextMessage);

		// 必须提交
		//session.commit();

		producer.close();
		session.close();
		connection.close();

	}


}
