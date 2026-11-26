package com.amy.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.amy.common.core.utils.StringUtils;
import com.amy.common.core.utils.file.FileUtils;
import com.amy.file.utils.FileUploadUtils;

/**
 * 本地文件存储
 *
 * <p><b>文件地址约定（换域名不改库的关键）</b>：本类返回的不是可访问 URL，
 * 而是不含协议/域名/端口、也不含网关前缀的<b>资源 key</b>，
 * 形如 {@code <file.prefix>/2026/11/26/xxx.jpg}。前缀只来自 Nacos {@code file.prefix}，
 * 与 {@link com.amy.file.config.ResourcesConfig} 的静态映射、网关白名单是同一个值。
 * 数据库只存 key，后端出站原样返回；访问地址由两端前端 API 适配层拼装。</p>
 *
 * @author amy
 */
@Primary
@RefreshScope
@Service
public class LocalSysFileServiceImpl implements ISysFileService
{
    /**
     * 资源 key 前缀（配置 file.prefix）
     */
    @Value("${file.prefix}")
    public String localFilePrefix;

    /**
     * 上传文件存储在本地的根路径
     */
    @Value("${file.path}")
    private String localFilePath;

    /**
     * 本地文件上传接口
     *
     * @param file 上传的文件
     * @return 资源 key（不是完整 URL），形如 {@code <file.prefix>/2026/11/26/xxx.jpg}
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile file) throws Exception
    {
        String name = FileUploadUtils.upload(localFilePath, file);
        return keyPrefix() + name;
    }

    /**
     * 本地文件删除接口
     *
     * <p>只接受按当前 {@code file.prefix} 生成的资源 key（完整 URL 会先取 path）。
     * 命中不了当前前缀的地址一律拒绝，避免拼出空路径或越界路径。</p>
     *
     * @param fileUrl 资源 key
     * @throws Exception
     */
    @Override
    public void deleteFile(String fileUrl) throws Exception
    {
        FileUtils.deleteFile(localFilePath + toLocalRelativePath(fileUrl));
    }

    /**
     * 把资源 key 转换成「上传根目录以下」的相对路径（以 / 开头）。
     * 结果始终拼在 localFilePath 之后，目录上跳在 Controller 的
     * FileUtils.validateFilePath 之外再做一次兜底校验。
     */
    private String toLocalRelativePath(String fileUrl)
    {
        if (!StringUtils.hasText(fileUrl))
        {
            throw new IllegalArgumentException("文件地址为空，不允许删除");
        }
        String path = fileUrl.trim();

        // 完整 URL -> 只保留 path 部分（通用 URL 解析，不针对任何域名）
        int schemeIndex = path.indexOf("://");
        if (schemeIndex >= 0)
        {
            int slashIndex = path.indexOf('/', schemeIndex + 3);
            path = slashIndex >= 0 ? path.substring(slashIndex) : "";
        }
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        int cut = (query >= 0 && fragment >= 0) ? Math.min(query, fragment) : (query >= 0 ? query : fragment);
        if (cut >= 0)
        {
            path = path.substring(0, cut);
        }

        String prefix = keyPrefix();
        if (!path.startsWith(prefix + "/"))
        {
            throw new IllegalArgumentException(StringUtils.format("资源文件({})非法，不允许删除。 ", fileUrl));
        }
        String relative = path.substring(prefix.length());
        if (!relative.startsWith("/") || relative.length() <= 1 || relative.contains(".."))
        {
            throw new IllegalArgumentException(StringUtils.format("资源文件({})非法，不允许删除。 ", fileUrl));
        }
        return relative;
    }

    /** 规整配置前缀：保证以 / 开头、不以 / 结尾，兼容写法不规范的 Nacos 配置 */
    private String keyPrefix()
    {
        String prefix = localFilePrefix == null ? "" : localFilePrefix.trim();
        if (!prefix.startsWith("/"))
        {
            prefix = "/" + prefix;
        }
        while (prefix.endsWith("/") && prefix.length() > 1)
        {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }
}
