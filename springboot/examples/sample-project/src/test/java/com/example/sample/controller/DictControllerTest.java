package com.example.sample.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sample.common.DictStatus;
import com.example.sample.common.ErrorCode;
import com.example.sample.entity.DictItem;
import com.example.sample.service.DictService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 字典接口切片测试：@WebMvcTest + mock Service（@MockitoBean），无需中间件。 */
@WebMvcTest(DictController.class)
class DictControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DictService dictService;

  @Test
  void shouldListDictItems() throws Exception {
    when(dictService.listDictItemsByType("CHANNEL")).thenReturn(List.of(item("APP", "应用商店")));

    mockMvc
        .perform(get("/dicts/CHANNEL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.data[0].itemCode").value("APP"))
        .andExpect(jsonPath("$.data[0].itemLabel").value("应用商店"));
  }

  @Test
  void shouldReturnEmptyListWhenTypeUnknown() throws Exception {
    when(dictService.listDictItemsByType("UNKNOWN")).thenReturn(List.of());

    mockMvc
        .perform(get("/dicts/UNKNOWN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  void shouldDisableDictItem() throws Exception {
    mockMvc
        .perform(post("/dicts/CHANNEL/items/APP/disable"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }

  @Test
  void shouldReturn404WhenDictItemMissing() throws Exception {
    doThrow(ErrorCode.DICT_ITEM_NOT_FOUND.exception("CHANNEL", "NOPE"))
        .when(dictService)
        .disableDictItem("CHANNEL", "NOPE");

    mockMvc
        .perform(post("/dicts/CHANNEL/items/NOPE/disable"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorCode.DICT_ITEM_NOT_FOUND.code()))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }

  private DictItem item(String itemCode, String itemLabel) {
    DictItem item = new DictItem();
    item.setTypeCode("CHANNEL");
    item.setItemCode(itemCode);
    item.setItemLabel(itemLabel);
    item.setStatus(DictStatus.ENABLED);
    return item;
  }
}
