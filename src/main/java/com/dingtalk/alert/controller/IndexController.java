package com.dingtalk.alert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 主页控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@Controller
public class IndexController {

    /**
     * 首页
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "钉钉告警系统");
        return "index";
    }

    /**
     * 数据库配置页面
     */
    @GetMapping("/database")
    public String database(Model model) {
        model.addAttribute("title", "数据库配置");
        return "database";
    }

    /**
     * 告警模板页面
     */
    @GetMapping("/template")
    public String template(Model model) {
        model.addAttribute("title", "告警模板");
        return "template";
    }

    /**
     * 查询任务页面
     */
    @GetMapping("/task")
    public String task(Model model) {
        model.addAttribute("title", "查询任务");
        return "task";
    }

    /**
     * 执行记录页面
     */
    @GetMapping("/record")
    public String record(Model model) {
        model.addAttribute("title", "执行记录");
        return "record";
    }

    /**
     * 钉钉配置页面
     */
    @GetMapping("/dingtalk")
    public String dingtalk(Model model) {
        model.addAttribute("title", "钉钉配置");
        return "dingtalk";
    }
}