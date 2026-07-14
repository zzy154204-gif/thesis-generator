package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.entity.Image;
import com.example.thesisgenerator.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 图片上传与访问 REST 控制器
 */
@RestController
@RequestMapping("/api/images")
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
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            Image image = imageService.upload(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", image.getId(),
                    "originalName", image.getOriginalName(),
                    "fileSize", image.getFileSize(),
                    "contentType", image.getContentType(),
                    "url", "/api/images/" + image.getId() + "/file"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取图片元信息
     * <p>
     * GET /api/images/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMetadata(@PathVariable Long id) {
        return imageService.getImage(id)
                .map(image -> ResponseEntity.ok(Map.of(
                        "id", image.getId(),
                        "originalName", image.getOriginalName(),
                        "fileSize", image.getFileSize(),
                        "contentType", image.getContentType(),
                        "createdAt", image.getCreatedAt(),
                        "url", "/api/images/" + image.getId() + "/file"
                )))
                .orElse(ResponseEntity.notFound().build());
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
                    // 获取 MIME 类型
                    String contentType = "image/png";
                    try {
                        contentType = resource.getURL().openConnection().getContentType();
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        imageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
