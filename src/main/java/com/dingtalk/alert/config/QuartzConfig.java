package com.dingtalk.alert.config;

import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Quartz配置类
 * 
 * @author system
 * @since 2024-01-01
 */
@Configuration
public class QuartzConfig {

    /**
     * 配置SchedulerFactoryBean
     * 
     * @param dataSource 数据源
     * @return SchedulerFactoryBean
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        
        // 设置数据源
        factory.setDataSource(dataSource);
        
        // 设置Quartz属性
        Properties properties = new Properties();
        
        // 调度器属性
        properties.setProperty("org.quartz.scheduler.instanceName", "DingTalkAlertScheduler");
        properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        
        // 线程池属性
        properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        properties.setProperty("org.quartz.threadPool.threadCount", "10");
        properties.setProperty("org.quartz.threadPool.threadPriority", "5");
        properties.setProperty("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
        
        // JobStore属性（使用内存存储）
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        
        // 如果需要持久化，可以使用数据库存储
        // properties.setProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        // properties.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        // properties.setProperty("org.quartz.jobStore.tablePrefix", "QRTZ_");
        // properties.setProperty("org.quartz.jobStore.isClustered", "false");
        
        factory.setQuartzProperties(properties);
        
        // 延迟启动
        factory.setStartupDelay(10);
        
        // 自动启动
        factory.setAutoStartup(true);
        
        // 覆盖已存在的任务
        factory.setOverwriteExistingJobs(true);
        
        return factory;
    }

    /**
     * 配置Scheduler
     * 
     * @param schedulerFactoryBean SchedulerFactoryBean
     * @return Scheduler
     */
    @Bean
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) {
        return schedulerFactoryBean.getScheduler();
    }
}