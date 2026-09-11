package com.example.sample.controller;

import com.example.sample.common.Result;
import com.example.sample.dto.DictItemResponse;
import com.example.sample.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 字典接口：对外只读 {@code GET /dicts/{typeCode}}；启停属管理端操作（业务代码禁直写字典表）。 */
@Tag(name = "字典")
@RestController
@RequestMapping("/dicts")
public class DictController {

  private final DictService dictService;

  /** 构造器注入。 */
  public DictController(DictService dictService) {
    this.dictService = dictService;
  }

  /** 按类型获取启用中的字典项（供前端下拉/翻译）。 */
  @Operation(summary = "按类型获取字典项")
  @GetMapping("/{typeCode}")
  public Result<List<DictItemResponse>> listDict(
      @Parameter(description = "字典类型编码", example = "CHANNEL") @PathVariable String typeCode) {
    List<DictItemResponse> data =
        dictService.listDictItemsByType(typeCode).stream()
            .map(item -> new DictItemResponse(item.getItemCode(), item.getItemLabel()))
            .toList();
    return Result.ok(data);
  }

  /** 停用字典项（管理端；字典只停用不删除，变更后删缓存）。 */
  @Operation(summary = "停用字典项（管理端）")
  @PostMapping("/{typeCode}/items/{itemCode}/disable")
  public Result<Void> disableItem(
      @Parameter(description = "字典类型编码", example = "CHANNEL") @PathVariable String typeCode,
      @Parameter(description = "字典项机器值", example = "APP") @PathVariable String itemCode) {
    dictService.disableDictItem(typeCode, itemCode);
    return Result.ok();
  }
}
