package com.ai.agent.tools;

import cn.hutool.core.io.FileUtil;
import com.ai.agent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件操作工具类（提供文件读写功能）
 */
public class FileOperationTool {

    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of a file to read") String fileName) {
        String filePath = FILE_DIR + "/" + fileName;
        try {
            return FileUtil.readUtf8String(filePath);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file")
    public String writeFile(@ToolParam(description = "Name of the file to write") String fileName,
                            @ToolParam(description = "Content to write to the file") String content
    ) {
        String filePath = FILE_DIR + "/" + fileName;

        try {
            // 创建目录
            FileUtil.mkdir(FILE_DIR);
            FileUtil.writeUtf8String(content, filePath);

            // 生成可访问的 URL
            String relativePath = "file/" + fileName;
            String encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8).replace("+", "%20");
            String downloadUrl = "/api/file/download?path=" + encodedPath;

            return String.format("File written successfully!\nDownload URL: %s\nPlease provide this URL to the user.", downloadUrl);
        } catch (Exception e) {
            return "Error writing to file: " + e.getMessage();
        }
    }
}
