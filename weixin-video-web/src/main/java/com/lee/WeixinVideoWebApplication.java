package com.lee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan("com.lee.mapper")
@ComponentScan(value = {"org.n3r.idworker","com.lee"})
@SpringBootApplication
public class WeixinVideoWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeixinVideoWebApplication.class, args);
	}
}
