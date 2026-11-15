package com.amy.sunpalaceartspace.mapper;

import com.amy.sunpalaceartspace.domain.entity.MinaNotice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知公告 数据层（MyBatis-Plus 模式，只读映射后台表 sys_notice）
 *
 * @author amy
 */
@Mapper
public interface MinaNoticeMapper extends BaseMapper<MinaNotice> {
}
