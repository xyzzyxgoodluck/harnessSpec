package com.example.sample.service.impl;

import com.example.sample.cache.DictCache;
import com.example.sample.common.DictStatus;
import com.example.sample.common.ErrorCode;
import com.example.sample.entity.DictItem;
import com.example.sample.entity.DictType;
import com.example.sample.mapper.DictMapper;
import com.example.sample.mapper.DictTypeMapper;
import com.example.sample.service.DictService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 字典服务实现：读走缓存、写仅限字典维护（业务代码禁直写字典表）。 */
@Service
public class DictServiceImpl implements DictService {

  private final DictMapper dictMapper;
  private final DictTypeMapper dictTypeMapper;
  private final DictCache dictCache;

  /** 构造器注入。 */
  public DictServiceImpl(
      DictMapper dictMapper, DictTypeMapper dictTypeMapper, DictCache dictCache) {
    this.dictMapper = dictMapper;
    this.dictTypeMapper = dictTypeMapper;
    this.dictCache = dictCache;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DictItem> listDictItemsByType(String typeCode) {
    return dictCache
        .findItems(typeCode)
        .orElseGet(
            () -> {
              List<DictItem> items = dictMapper.selectDictItemsByType(typeCode, DictStatus.ENABLED);
              dictCache.putItems(typeCode, items);
              return items;
            });
  }

  @Override
  @Transactional
  public void disableDictItem(String typeCode, String itemCode) {
    DictType type = dictTypeMapper.selectDictTypeByCode(typeCode);
    if (type == null) {
      throw ErrorCode.DICT_TYPE_NOT_FOUND.exception(typeCode);
    }
    int updated = dictMapper.updateDictItemStatus(typeCode, itemCode, DictStatus.DISABLED);
    if (updated == 0) {
      throw ErrorCode.DICT_ITEM_NOT_FOUND.exception(typeCode, itemCode);
    }
    dictCache.evictItemsAfterCommit(typeCode);
  }
}
