package com.dingtalk.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dingtalk.alert.entity.AlertTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 告警模板Mapper接口
 * 
 * @author system
 * @since 2024-01-01
 */
@Mapper
public interface AlertTemplateMapper extends BaseMapper<AlertTemplate> {

    /**
     * 查询所有启用的告警模板
     * 
     * @return 告警模板列表
     */
    @Select("SELECT * FROM alert_template WHERE enabled = 1 AND deleted = 0 ORDER BY create_time DESC")
    List<AlertTemplate> selectEnabledTemplates();

    /**
     * 根据模板名称查询
     * 
     * @param templateName 模板名称
     * @return 告警模板
     */
    @Select("SELECT * FROM alert_template WHERE template_name = #{templateName} AND deleted = 0")
    AlertTemplate selectByTemplateName(String templateName);
}