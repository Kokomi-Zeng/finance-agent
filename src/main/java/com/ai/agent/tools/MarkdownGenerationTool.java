package com.ai.agent.tools;

import cn.hutool.core.io.FileUtil;
import com.ai.agent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Markdown 文档生成工具
 */
public class MarkdownGenerationTool {

    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/markdown";

    @Tool(description = "Generate a Markdown file with given content. Use this to create formatted documents, reports, or notes.")
    public String generateMarkdown(
            @ToolParam(description = "Name of the file (should end with .md)") String fileName,
            @ToolParam(description = "Content to be written in Markdown format") String content) {

        // 确保文件名以 .md 结尾
        if (!fileName.endsWith(".md")) {
            fileName = fileName + ".md";
        }

        String filePath = FILE_DIR + "/" + fileName;

        try {
            // 创建目录
            FileUtil.mkdir(FILE_DIR);
            // 写入 Markdown 内容
            FileUtil.writeUtf8String(content, filePath);

            // 生成可访问的 URL
            String relativePath = "markdown/" + fileName;
            String encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8).replace("+", "%20");
            String downloadUrl = "/api/file/download?path=" + encodedPath;
            String previewUrl = "/api/file/preview?path=" + encodedPath;

            return String.format("Markdown file generated successfully!\n" +
                    "Download URL: %s\n" +
                    "Preview URL: %s\n" +
                    "Please provide these URLs to the user so they can access the file.",
                    downloadUrl, previewUrl);
        } catch (Exception e) {
            return "Error generating Markdown file: " + e.getMessage();
        }
    }

    @Tool(description = "Read content from an existing Markdown file")
    public String readMarkdown(
            @ToolParam(description = "Name of the Markdown file to read") String fileName) {

        // 确保文件名以 .md 结尾
        if (!fileName.endsWith(".md")) {
            fileName = fileName + ".md";
        }

        String filePath = FILE_DIR + "/" + fileName;

        try {
            if (!FileUtil.exist(filePath)) {
                return "File not found: " + filePath;
            }
            return FileUtil.readUtf8String(filePath);
        } catch (Exception e) {
            return "Error reading Markdown file: " + e.getMessage();
        }
    }
}
