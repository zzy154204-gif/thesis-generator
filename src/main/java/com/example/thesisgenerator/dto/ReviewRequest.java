package com.example.thesisgenerator.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    private String commentHtml;

    @NotNull(message = "评分不能为空")
    @Min(value = 0, message = "评分最低为0")
    @Max(value = 100, message = "评分最高为100")
    private Integer score;

    private String grade;

    @NotBlank(message = "批阅操作不能为空")
    private String action; // REVIEWED 或 RETURNED

    private String returnReason;
}
