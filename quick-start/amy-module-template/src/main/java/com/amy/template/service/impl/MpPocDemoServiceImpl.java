package com.amy.template.service.impl;

import org.springframework.stereotype.Service;
import com.amy.template.domain.entity.MpPocDemo;
import com.amy.template.mapper.MpPocDemoMapper;
import com.amy.template.service.IMpPocDemoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

/**
 * MP 集成验证 Service 实现（POC 模板）。
 *
 * @author amy
 */
@Service
public class MpPocDemoServiceImpl extends ServiceImpl<MpPocDemoMapper, MpPocDemo> implements IMpPocDemoService
{
    @Override
    public IPage<MpPocDemo> selectMpPocDemoList(IPage<MpPocDemo> page, MpPocDemo entity)
    {
        return baseMapper.selectMpPocDemoList(page, entity);
    }
}
