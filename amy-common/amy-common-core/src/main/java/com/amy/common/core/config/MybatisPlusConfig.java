package com.amy.common.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 集成配置（替代原 mybatis-spring-boot-starter）。
 * <p>
 * 说明：
 * 1. 分页插件使用 MP 的 MybatisPlusInterceptor + PaginationInnerInterceptor（MySQL 方言）。
 *    存量代码仍使用 PageHelper，二者通过不同机制生效、互不干扰（不要在同一查询上混用 startPage 与 IPage）。
 * 2. 通过 MybatisPlusPropertiesCustomizer 把 type-aliases-package / mapper-locations
 *    设置为与原有 Nacos mybatis: 配置一致的值，确保存量 XML 映射（resultMap type="Xxx" 等）继续生效。
 * 3. 数据权限 @DataScope 是切面（在参数 BaseEntity.params 中注入 SQL 片段），与 MP 无关，
 *    需要数据权限的查询仍走「自定义 Mapper 方法 + XML ${params.dataScope}」即可。
 * </p>
 *
 * @author amy
 */
@Configuration
public class MybatisPlusConfig
{
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor()
    {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // MySQL 分页方言；如需其他库可在此扩展
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer()
    {
        return properties -> {
            // 与原有 Nacos mybatis.type-aliases-package 保持一致，保证存量 resultMap 短类名解析
            properties.setTypeAliasesPackage("com.amy.**.domain");
            // 与原有 Nacos mybatis.mapper-locations 保持一致
            properties.setMapperLocations(new String[] { "classpath*:mapper/**/*Mapper.xml" });
        };
    }
}
