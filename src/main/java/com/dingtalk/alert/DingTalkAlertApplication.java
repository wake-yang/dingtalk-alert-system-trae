package com.dingtalk.alert;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 钉钉告警系统主启动类
 * 
 * @author system
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.dingtalk.alert.mapper")
public class DingTalkAlertApplication {

    public static void main(String[] args) {
        SpringApplication.run(DingTalkAlertApplication.class, args);
        System.out.println("\n==================================");
        System.out.println("钉钉告警系统启动成功！");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("==================================");
    }

}