package com.example.thesisgenerator.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AnnotationRequest {

    @NotNull(message = "论文ID不能为空")
    private Long thesisId;

    @NotNull(message = "章节ID不能为空")
    private Long sectionId;

    @NotNull(message = "起始偏移不能为空")
    @Min(value = 0, message = "起始偏移不能为负数")
    private Integer startOffset;

    @NotNull(message = "文本长度不能为空")
    @Positive(message = "文本长度必须为正数")
    private Integer textLength;

    private String selectedText;

    @NotBlank(message = "批注内容不能为空")
    private String content;
}
