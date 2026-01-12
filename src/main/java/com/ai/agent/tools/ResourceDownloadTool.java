package com.ai.agent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.ai.agent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 资源下载工具
 */
public class ResourceDownloadTool {

    @Tool(description = "Download a file from a direct download URL. IMPORTANT: The URL must point directly to a file (e.g., .pdf, .xlsx, .zip), not a web page. If you get an HTML page error, use scrapeWebPage first to find the actual download link.")
    public String downloadResource(
            @ToolParam(description = "Direct download URL of the file (must be a file URL, not a directory or web page)") String url,
            @ToolParam(description = "Name to save the file as (include extension, e.g., report.pdf)") String fileName) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/download";
        String filePath = fileDir + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);

            // 1. 先检查 URL 的 Content-Type
            cn.hutool.http.HttpResponse headResponse = HttpUtil.createGet(url).execute();
            String contentType = headResponse.header("Content-Type");

            // 检测是否是 HTML 页面（目录索引、错误页等）
            if (contentType != null && contentType.toLowerCase().contains("text/html")) {
                return String.format(
                    "Error: URL points to an HTML page, not a downloadable file.\n" +
                    "Content-Type: %s\n" +
                    "This is likely a directory index or web page. Please:\n" +
                    "1. Use scrapeWebPage to extract the actual file download link\n" +
                    "2. Or verify the URL points directly to a file (e.g., .pdf, .zip, .xlsx)",
                    contentType
                );
            }

            // 2. 下载文件
            HttpUtil.downloadFile(url, new File(filePath));

            // 3. 验证下载的文件不是 HTML
            File downloadedFile = new File(filePath);
            if (downloadedFile.exists() && downloadedFile.length() < 10000) {
                // 小于 10KB 的文件可能是错误页面，检查内容
                String fileContent = FileUtil.readUtf8String(downloadedFile);
                if (fileContent.trim().startsWith("<!DOCTYPE") ||
                    fileContent.trim().startsWith("<html") ||
                    fileContent.contains("<title>Index of /</title>")) {
                    // 删除错误文件
                    FileUtil.del(downloadedFile);
                    return String.format(
                        "Error: Downloaded file is an HTML page, not the intended resource.\n" +
                        "The URL may be incorrect or requires authentication.\n" +
                        "File preview: %s...\n" +
                        "Please verify the download link is correct.",
                        fileContent.substring(0, Math.min(200, fileContent.length()))
                    );
                }
            }

            // 生成可访问的 URL
            String relativePath = "download/" + fileName;
            String encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8).replace("+", "%20");
            String downloadUrl = "/api/file/download?path=" + encodedPath;

            return String.format(
                "Resource downloaded successfully!\n" +
                "File size: %d bytes\n" +
                "Download URL: %s\n" +
                "Please provide this URL to the user.",
                downloadedFile.length(),
                downloadUrl
            );
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}
