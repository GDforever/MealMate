package com.gd.mealmate.controller;

import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "Images", description = "图片文件访问接口")
public class ImageController {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @GetMapping("/food/{userId}/{filename}")
    @Operation(summary = "获取上传的食物图片")
    public ResponseEntity<Resource> getFoodImage(
            @PathVariable Long userId,
            @PathVariable String filename) {
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = basePath.resolve("food").resolve(String.valueOf(userId)).resolve(filename).normalize();

        if (!filePath.startsWith(basePath)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        if (!Files.exists(filePath)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Resource resource = new FileSystemResource(filePath);
        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (Exception e) {
            contentType = "image/jpeg";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }
}
