package com.example.sample.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.sample.cache.DictCache;
import com.example.sample.common.BizException;
import com.example.sample.common.DictStatus;
import com.example.sample.common.ErrorCode;
import com.example.sample.entity.DictItem;
import com.example.sample.entity.DictType;
import com.example.sample.mapper.DictMapper;
import com.example.sample.mapper.DictTypeMapper;
import com.example.sample.service.impl.DictServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 字典服务单测：读缓存/回源、启停与删缓存、字典类型/项不存在的异常分支（断言 ErrorCode）。 */
@ExtendWith(MockitoExtension.class)
class DictServiceImplTest {

  @Mock private DictMapper dictMapper;
  @Mock private DictTypeMapper dictTypeMapper;
  @Mock private DictCache dictCache;

  private DictServiceImpl dictService;

  @BeforeEach
  void setUp() {
    dictService = new DictServiceImpl(dictMapper, dictTypeMapper, dictCache);
  }

  @Test
  void shouldReturnCachedItemsWhenCacheHit() {
    List<DictItem> cached = List.of(item("APP", "应用商店"));
    when(dictCache.findItems("CHANNEL")).thenReturn(Optional.of(cached));

    assertThat(dictService.listDictItemsByType("CHANNEL")).isSameAs(cached);
    verify(dictMapper, never()).selectDictItemsByType(any(), any(Integer.class));
  }

  @Test
  void shouldLoadFromDbAndFillCacheWhenMiss() {
    List<DictItem> fromDb = List.of(item("APP", "应用商店"));
    when(dictCache.findItems("CHANNEL")).thenReturn(Optional.empty());
    when(dictMapper.selectDictItemsByType("CHANNEL", DictStatus.ENABLED)).thenReturn(fromDb);

    assertThat(dictService.listDictItemsByType("CHANNEL")).isEqualTo(fromDb);
    verify(dictCache).putItems("CHANNEL", fromDb);
  }

  @Test
  void shouldReturnEmptyWhenTypeHasNoItem() {
    when(dictCache.findItems("UNKNOWN")).thenReturn(Optional.empty());
    when(dictMapper.selectDictItemsByType("UNKNOWN", DictStatus.ENABLED)).thenReturn(List.of());

    assertThat(dictService.listDictItemsByType("UNKNOWN")).isEmpty();
  }

  @Test
  void shouldDisableItemAndEvictCache() {
    when(dictTypeMapper.selectDictTypeByCode("CHANNEL")).thenReturn(type("CHANNEL"));
    when(dictMapper.updateDictItemStatus("CHANNEL", "APP", DictStatus.DISABLED)).thenReturn(1);

    dictService.disableDictItem("CHANNEL", "APP");

    verify(dictCache).evictItemsAfterCommit("CHANNEL");
  }

  @Test
  void shouldRejectDisableWhenDictTypeMissing() {
    when(dictTypeMapper.selectDictTypeByCode("NOPE")).thenReturn(null);

    assertThatThrownBy(() -> dictService.disableDictItem("NOPE", "APP"))
        .isInstanceOf(BizException.class)
        .extracting(e -> ((BizException) e).getErrorCode())
        .isEqualTo(ErrorCode.DICT_TYPE_NOT_FOUND);
    verify(dictCache, never()).evictItemsAfterCommit(any());
  }

  @Test
  void shouldRejectDisableWhenDictItemMissing() {
    when(dictTypeMapper.selectDictTypeByCode("CHANNEL")).thenReturn(type("CHANNEL"));
    when(dictMapper.updateDictItemStatus("CHANNEL", "NOPE", DictStatus.DISABLED)).thenReturn(0);

    assertThatThrownBy(() -> dictService.disableDictItem("CHANNEL", "NOPE"))
        .isInstanceOf(BizException.class)
        .extracting(e -> ((BizException) e).getErrorCode())
        .isEqualTo(ErrorCode.DICT_ITEM_NOT_FOUND);
    verify(dictCache, never()).evictItemsAfterCommit(any());
  }

  private DictItem item(String itemCode, String itemLabel) {
    DictItem item = new DictItem();
    item.setTypeCode("CHANNEL");
    item.setItemCode(itemCode);
    item.setItemLabel(itemLabel);
    item.setStatus(DictStatus.ENABLED);
    return item;
  }

  private DictType type(String typeCode) {
    DictType type = new DictType();
    type.setTypeCode(typeCode);
    type.setTypeName("下单渠道");
    return type;
  }
}
