package com.dingtalk.alert.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;

@Service
public class DashboardService {
    
    @Cacheable(value = "dashboard", key = "'overview'", unless = "#result == null")
    public Map<String, Object> getCachedDashboardData() {
        // 这里调用实际的数据获取逻辑
        return getDashboardData();
    }
    
    private Map<String, Object> getDashboardData() {
        // 实际的数据获取逻辑
        Map<String, Object> data = new HashMap<>();
        // ... 数据获取逻辑
        return data;
    }
}