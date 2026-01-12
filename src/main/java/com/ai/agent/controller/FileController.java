package com.ai.agent.controller;

import cn.hutool.core.io.FileUtil;
import com.ai.agent.constant.FileConstant;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件下载控制器
 */
@RestController
@RequestMapping("/file")
public class FileController {

    /**
     * 下载文件
     *
     * @param path 相对于 tmp 目录的文件路径，如 markdown/london_guide.md
     * @return 文件资源
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String path) {
        // 安全检查：防止路径遍历攻击
        if (path.contains("..") || path.startsWith("/") || path.startsWith("\\")) {
            return ResponseEntity.badRequest().build();
        }

        String filePath = FileConstant.FILE_SAVE_DIR + "/" + path;
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        // 确保文件在允许的目录内
        try {
            String canonicalPath = file.getCanonicalPath();
            String allowedDir = new File(FileConstant.FILE_SAVE_DIR).getCanonicalPath();
            if (!canonicalPath.startsWith(allowedDir)) {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = new FileSystemResource(file);
        String fileName = file.getName();
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        // 根据文件类型设置 Content-Type
        String contentType = getContentType(fileName);

        // 构建 Content-Disposition，避免中文字符编码问题
        // 使用 filename* 参数支持 UTF-8 编码的文件名
        String contentDisposition = "attachment; filename*=UTF-8''" + encodedFileName;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    /**
     * 预览文件（在线查看，不下载）
     *
     * @param path 相对于 tmp 目录的文件路径
     * @return 文件内容
     */
    @GetMapping("/preview")
    public ResponseEntity<String> previewFile(@RequestParam String path) {
        // 安全检查：防止路径遍历攻击
        if (path.contains("..") || path.startsWith("/") || path.startsWith("\\")) {
            return ResponseEntity.badRequest().build();
        }

        String filePath = FileConstant.FILE_SAVE_DIR + "/" + path;
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        // 确保文件在允许的目录内
        try {
            String canonicalPath = file.getCanonicalPath();
            String allowedDir = new File(FileConstant.FILE_SAVE_DIR).getCanonicalPath();
            if (!canonicalPath.startsWith(allowedDir)) {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String content = FileUtil.readUtf8String(file);
            // 设置 UTF-8 编码，解决中文乱码问题
            MediaType mediaType = new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error reading file: " + e.getMessage());
        }
    }

    private String getContentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".md")) {
            return "text/markdown";
        } else if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerName.endsWith(".json")) {
            return "application/json";
        } else if (lowerName.endsWith(".html")) {
            return "text/html";
        } else {
            return "application/octet-stream";
        }
    }
}
