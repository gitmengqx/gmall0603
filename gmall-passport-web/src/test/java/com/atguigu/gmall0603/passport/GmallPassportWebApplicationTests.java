package com.atguigu.gmall0603.passport;

import com.atguigu.gmall0603.passport.config.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void testJWT(){
		String key = "atguigu";
		HashMap<String, Object> map = new HashMap<>();
		map.put("userId","1001");
		map.put("nickName","hello");
		String salt = "192.168.67.224";
		String token = JwtUtil.encode(key, map, salt);
		System.out.println("token:"+token);
		Map<String, Object> objectMap = JwtUtil.decode(token, key, "122345");
		System.out.println(objectMap);
	}
}
