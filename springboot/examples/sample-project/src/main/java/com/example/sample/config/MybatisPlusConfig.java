package com.example.sample.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 配置：分页插件（缺它=无 LIMIT）与乐观锁插件（缺它=@Version 静默失效）集中声明。 */
@Configuration
public class MybatisPlusConfig {

  /** 分页 + 乐观锁内部拦截器；顺序：分页在乐观锁之前。 */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    return interceptor;
  }

  /** 审计字段自动填充（create_time/update_time，对应 CODING_STANDARDS §5 审计字段约定）。 */
  @Bean
  public MetaObjectHandler auditMetaObjectHandler() {
    return new AuditMetaObjectHandler();
  }
}
