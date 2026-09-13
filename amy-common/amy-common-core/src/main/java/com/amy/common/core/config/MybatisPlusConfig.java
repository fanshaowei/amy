package com.amy.common.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.type.TypeAliasRegistry;
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
 * 4. MybatisPlus 3.5.17 的 MybatisPlusProperties.setTypeAliasesPackage 仅接受单 String
 *    且 TypeAliasRegistry.registerAliases 不解析逗号分隔的多包。
 *    为兼容新模块按 §5.5.1 拆分出的 domain 子包，另通过 ConfigurationCustomizer 显式
 *    registerAliases 每一条子包，让短类名（如 resultMap type="ReservationOrder"）也能解析。
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
            // 与原 Nacos mybatis.type-aliases-package 保持一致，保证旧 RuoYi 子包结构
            // （com.amy.{module}.domain.*）的短类名继续解析。
            // 新增的 entity/vo/req/resp/bo/criteria 子包由下面 domainSubpackageAliasCustomizer 注册。
            properties.setTypeAliasesPackage("com.amy.**.domain");
            // 与原 Nacos mybatis.mapper-locations 保持一致
            properties.setMapperLocations(new String[] { "classpath*:mapper/**/*Mapper.xml" });
            // 全局逻辑删除取值（项目约定）：0=存在 1=删除（覆盖若依的 0/2 约定；RuoYi 系统表 sys_* 仍走 0/2）。
            // （BaseEntity.delFlag 已用 @TableLogic 标注，此处兜底，确保任何未显式赋值的 delFlag 字段一致。）
            GlobalConfig.DbConfig dbConfig = properties.getGlobalConfig().getDbConfig();
            if (dbConfig != null)
            {
                dbConfig.setLogicNotDeleteValue("0");
                dbConfig.setLogicDeleteValue("1");
            }
        };
    }

    /**
     * 注册新模块按 §5.5.1 拆分出的 domain 子包，使短类名（如 {@code type="ReservationOrder"}、
     * {@code type="ReservationOrderVO"}、{@code type="ReservationOrderReq"}）在 XML 中能直接解析。
     */
    @Bean
    public ConfigurationCustomizer domainSubpackageAliasCustomizer()
    {
        return configuration -> {
            TypeAliasRegistry registry = configuration.getTypeAliasRegistry();
            registry.registerAliases("com.amy.**.entity");
        };
    }
}
