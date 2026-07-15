package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.Image;
import com.example.thesisgenerator.repository.ImageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 图片业务逻辑层
 * <p>
 * 处理图片上传、存储、读取，支持 jpg/png/gif/webp 格式。
 */
@Service
@RequiredArgsConstructor
public class ImageService {

    /** 允许上传的图片类型 */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /** 最大文件大小（10MB） */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    private final ImageRepository imageRepository;

    /** 上传文件存储目录，从配置文件读取 */
    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    /** 上传文件存储的绝对路径 */
    private Path uploadPath;

    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            throw new BusinessException("无法创建上传目录: " + this.uploadPath);
        }
    }

    /**
     * 上传图片
     *
     * @param file 上传的文件
     * @return 保存后的 Image 实体
     */
    public Image upload(MultipartFile file) {
        // 校验文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(400, "不支持的图片类型，仅允许: jpg/png/gif/webp");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小超出限制（最大 10MB）");
        }

        // 生成唯一文件名（UUID 防止重名）
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID().toString() + extension;
        Path targetPath = this.uploadPath.resolve(storedName);

        // 保存文件到磁盘
        try (java.io.InputStream in = file.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + originalName);
        }

        // 保存元信息到数据库
        Image image = Image.builder()
                .originalName(originalName != null ? originalName : "unknown")
                .storedName(storedName)
                .filePath(targetPath.toString())
                .fileSize(file.getSize())
                .contentType(contentType)
                .build();

        return imageRepository.save(image);
    }

    /**
     * 根据 ID 获取图片元信息
     */
    public Optional<Image> getImage(Long id) {
        return imageRepository.findById(id);
    }

    /**
     * 根据 ID 获取图片文件资源
     */
    public Optional<Resource> getFile(Long id) {
        return imageRepository.findById(id)
                .map(image -> {
                    try {
                        Path filePath = Paths.get(image.getFilePath());
                        Resource resource = new UrlResource(filePath.toUri());
                        if (resource.exists() && resource.isReadable()) {
                            return resource;
                        }
                        return null;
                    } catch (MalformedURLException e) {
                        return null;
                    }
                });
    }

    /**
     * 根据 ID 删除图片
     */
    public void delete(Long id) {
        imageRepository.findById(id).ifPresent(image -> {
            // 删除磁盘文件
            try {
                Files.deleteIfExists(Paths.get(image.getFilePath()));
            } catch (IOException e) {
                // 文件删除失败不影响数据库删除
            }
            // 删除数据库记录
            imageRepository.deleteById(id);
        });
    }
}
