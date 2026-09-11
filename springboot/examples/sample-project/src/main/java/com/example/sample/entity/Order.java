package com.example.sample.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.example.sample.common.OrderStatus;
import java.time.LocalDateTime;

/** 订单实体（与表 t_order 对应；纯数据载体，禁直接出参）。 */
@TableName("t_order")
public class Order {

  /** 主键：全库统一雪花（IdType.ASSIGN_ID），与 db/V1__init.sql 的 BIGINT 主键对齐。 */
  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String orderNo;

  /** 状态机：落库为字符串 code（OrderStatus 枚举，稳定状态机不落字典表）。 */
  private OrderStatus status;

  private Long amountFen;

  /** 乐观锁：需注册 OptimisticLockerInnerInterceptor 才生效（见 MybatisPlusConfig）。 */
  @Version private Integer version;

  /** 逻辑删除（0=正常，1=已删）。 */
  @TableLogic private Integer deleted;

  /** 审计字段：由 AuditMetaObjectHandler 自动填充。 */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  /** 审计字段：由 AuditMetaObjectHandler 自动填充。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOrderNo() {
    return orderNo;
  }

  public void setOrderNo(String orderNo) {
    this.orderNo = orderNo;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  public Long getAmountFen() {
    return amountFen;
  }

  public void setAmountFen(Long amountFen) {
    this.amountFen = amountFen;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Integer getDeleted() {
    return deleted;
  }

  public void setDeleted(Integer deleted) {
    this.deleted = deleted;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }
}
