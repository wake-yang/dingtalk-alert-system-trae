package com.dingtalk.alert.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring应用上下文提供者
 * 用于在非Spring管理的类中获取ApplicationContext
 * 
 * @author system
 * @since 2024-01-01
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {
    
    private static ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }
    
    /**
     * 获取ApplicationContext
     * 
     * @return ApplicationContext实例
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    
    /**
     * 根据类型获取Bean
     * 
     * @param clazz Bean的类型
     * @param <T> 泛型类型
     * @return Bean实例
     */
    public static <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(clazz);
    }
    
    /**
     * 根据名称获取Bean
     * 
     * @param name Bean的名称
     * @return Bean实例
     */
    public static Object getBean(String name) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(name);
    }
    
    /**
     * 根据名称和类型获取Bean
     * 
     * @param name Bean的名称
     * @param clazz Bean的类型
     * @param <T> 泛型类型
     * @return Bean实例
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(name, clazz);
    }
}