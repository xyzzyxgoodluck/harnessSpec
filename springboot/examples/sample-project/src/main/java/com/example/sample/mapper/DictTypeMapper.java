package com.example.sample.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.sample.entity.DictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 字典类型 Mapper：只读查询（与 DictMapper 同属字典基础设施，仅 DictService 可依赖）。 */
@Mapper
public interface DictTypeMapper extends BaseMapper<DictType> {

  /** 按 type_code 查询字典类型（列显式、禁 SELECT *；新字典类型先查重用本方法）。 */
  @Select(
      "SELECT id, type_code, type_name, status, sort_no, remark, version, deleted "
          + "FROM t_dict_type WHERE deleted = 0 AND type_code = #{typeCode}")
  DictType selectDictTypeByCode(@Param("typeCode") String typeCode);
}
