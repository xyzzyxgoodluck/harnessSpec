package com.example.sample.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.sample.entity.DictItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 字典项 Mapper：读只读、写仅限字典维护（业务代码禁直写字典表，CODING_STANDARDS §5）。 */
@Mapper
public interface DictMapper extends BaseMapper<DictItem> {

  /** 按类型查询字典项（列显式、禁 SELECT *；排序依赖 sort_no）。 */
  @Select(
      "SELECT id, type_code, item_code, item_label, status, sort_no, remark, version, deleted "
          + "FROM t_dict_item WHERE deleted = 0 AND type_code = #{typeCode} "
          + "AND status = #{status} ORDER BY sort_no, id")
  List<DictItem> selectDictItemsByType(
      @Param("typeCode") String typeCode, @Param("status") int status);

  /** 启停字典项（只停用不删除）；返回受影响行数供调用方判定存在性。 */
  @Update(
      "UPDATE t_dict_item SET status = #{status}, update_time = NOW(3) "
          + "WHERE deleted = 0 AND type_code = #{typeCode} AND item_code = #{itemCode}")
  int updateDictItemStatus(
      @Param("typeCode") String typeCode,
      @Param("itemCode") String itemCode,
      @Param("status") int status);
}
