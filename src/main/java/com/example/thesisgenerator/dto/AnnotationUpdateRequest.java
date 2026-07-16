package com.example.thesisgenerator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnotationUpdateRequest {

    @NotBlank(message = "批注内容不能为空")
    private String content;
}
