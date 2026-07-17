package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.Image;
import com.example.thesisgenerator.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 图片上传与访问 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /**
     * 上传图片
     * <p>
     * POST /api/images/upload
     * Content-Type: multipart/form-data
     * 参数: file (MultipartFile)
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        Image image = imageService.upload(file);
        return Result.ok(Map.of(
                "id", image.getId(),
                "originalName", image.getOriginalName(),
                "fileSize", image.getFileSize(),
                "contentType", image.getContentType(),
                "url", "/api/images/" + image.getId() + "/file"
        ));
    }

    /**
     * 获取图片元信息
     * <p>
     * GET /api/images/{id}
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getMetadata(@PathVariable Long id) {
        return imageService.getImage(id)
                .map(image -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", image.getId());
                    result.put("originalName", image.getOriginalName());
                    result.put("fileSize", image.getFileSize());
                    result.put("contentType", image.getContentType());
                    result.put("createdAt", image.getCreatedAt() != null ? image.getCreatedAt().toString() : null);
                    result.put("url", "/api/images/" + image.getId() + "/file");
                    return Result.ok(result);
                })
                .orElse(Result.error(404, "图片不存在"));
    }

    /**
     * 获取图片文件（用于在浏览器中显示）
     * <p>
     * GET /api/images/{id}/file
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getFile(@PathVariable Long id) {
        return imageService.getFile(id)
                .map(resource -> {
                    String contentType = "image/png";
                    try {
                        String detected = resource.getURL().openConnection().getContentType();
                        if (detected != null && !detected.isBlank()) {
                            contentType = detected;
                        }
                    } catch (Exception ignored) {
                    }

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除图片
     * <p>
     * DELETE /api/images/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        imageService.delete(id);
        return Result.ok();
    }
}
