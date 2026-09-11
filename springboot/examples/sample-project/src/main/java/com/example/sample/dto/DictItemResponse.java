package com.example.sample.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 字典项出参。
 *
 * @param itemCode 机器值（UPPER_SNAKE_CASE）
 * @param itemLabel 中文展示名
 */
@Schema(description = "字典项")
public record DictItemResponse(
    @Schema(description = "机器值", example = "APP") String itemCode,
    @Schema(description = "中文展示名", example = "应用商店") String itemLabel) {}
