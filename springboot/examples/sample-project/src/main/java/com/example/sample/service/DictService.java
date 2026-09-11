package com.example.sample.service;

import com.example.sample.entity.DictItem;
import java.util.List;

/** 字典只读服务（写/维护走管理入口，业务侧仅只读）：编码细则见 CODING_STANDARDS §5「业务字典」。 */
public interface DictService {

  /** 按类型查询启用中的字典项（带 Redis 缓存，未命中回源并回填）。 */
  List<DictItem> listDictItemsByType(String typeCode);

  /** 停用字典项（字典只停用不删除），并删除该类型缓存。 */
  void disableDictItem(String typeCode, String itemCode);
}
