package com.example.sample.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;

/** 审计字段填充：insert 填 create_time/update_time，update 刷新 update_time（CODING_STANDARDS §5）。 */
public class AuditMetaObjectHandler implements MetaObjectHandler {

  @Override
  public void insertFill(MetaObject metaObject) {
    LocalDateTime now = LocalDateTime.now();
    strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
    strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
  }

  @Override
  public void updateFill(MetaObject metaObject) {
    strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
  }
}
