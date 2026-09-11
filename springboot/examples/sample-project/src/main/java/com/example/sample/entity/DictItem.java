package com.example.sample.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;

/** 字典项实体（与 t_dict_item 对应；唯一约束 {@code (type_code, item_code)}）。 */
@TableName("t_dict_item")
public class DictItem {

  /** 主键：全库统一雪花（IdType.ASSIGN_ID），与 DDL 对齐。 */
  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String typeCode;
  private String itemCode;
  private String itemLabel;

  /** 启停状态（只停用不删除，见 DictStatus）。 */
  private Integer status;

  /** 排序号（DictMapper 的 ORDER BY sort_no, id 依赖本列）。 */
  private Integer sortNo;

  private String remark;

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

  public String getTypeCode() {
    return typeCode;
  }

  public void setTypeCode(String typeCode) {
    this.typeCode = typeCode;
  }

  public String getItemCode() {
    return itemCode;
  }

  public void setItemCode(String itemCode) {
    this.itemCode = itemCode;
  }

  public String getItemLabel() {
    return itemLabel;
  }

  public void setItemLabel(String itemLabel) {
    this.itemLabel = itemLabel;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Integer getSortNo() {
    return sortNo;
  }

  public void setSortNo(Integer sortNo) {
    this.sortNo = sortNo;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
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
