package com.amy.template.service;

import com.amy.template.domain.entity.MpPocDemo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * MP 集成验证 Service（POC 模板）。
 * <p>
 * 注意：IService / ServiceImpl 来自 mybatis-plus-spring（Boot4 适配包），包路径为
 * com.baomidou.mybatisplus.spring.service(+.impl)。
 * </p>
 *
 * @author amy
 */
public interface IMpPocDemoService extends IService<MpPocDemo>
{
    /**
     * 分页列表（带数据权限）
     */
    IPage<MpPocDemo> selectMpPocDemoList(IPage<MpPocDemo> page, MpPocDemo entity);
}
