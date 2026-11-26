package com.amy.file.service;

import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.nacos.common.utils.IoUtils;
import com.amy.common.core.utils.StringUtils;
import com.amy.file.config.MinioConfig;
import com.amy.file.utils.FileUploadUtils;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

/**
 * Minio 文件存储
 *
 * <p><b>注意</b>：当前 {@link LocalSysFileServiceImpl} 标注了 @Primary，本实现未启用。
 * 本类返回值与本地实现统一为<b>资源 key</b> {@code <file.prefix>/2026/11/26/xxx.jpg}，
 * 前缀同样只来自 Nacos {@code file.prefix}，不含域名、bucket、网关前缀。</p>
 *
 * <p><b>启用前置条件</b>：ResourcesConfig 的 {@code <file.prefix>/**} 只映射到本地磁盘，
 * 切换到 MinIO 前必须先在 amy-file 增加该前缀的流式回源端点（或由 nginx 直连 MinIO），
 * 否则上传后拿到的 key 无法被访问。这一步未做之前不要移除 Local 的 @Primary。</p>
 *
 * @author amy
 */
@Service
public class MinioSysFileServiceImpl implements ISysFileService
{
    @Value("${file.prefix}")
    private String localFilePrefix;

    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private MinioClient client;

    /**
     * Minio文件上传接口
     *
     * @param file 上传的文件
     * @return 资源 key（不是完整 URL），形如 {@code <file.prefix>/2026/11/26/xxx.jpg}
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile file) throws Exception
    {
        InputStream inputStream = null;
        try
        {
            String fileName = FileUploadUtils.extractFilename(file);
            inputStream = file.getInputStream();
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            client.putObject(args);
            return keyPrefix() + "/" + fileName;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Minio Failed to upload file", e);
        }
        finally
        {
            IoUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Minio文件删除接口
     *
     * <p>只接受按当前 {@code file.prefix} 生成的资源 key（完整 URL 会先取 path）。</p>
     *
     * @param fileUrl 资源 key
     * @throws Exception
     */
    @Override
    public void deleteFile(String fileUrl) throws Exception
    {
        try
        {
            String objectName = toObjectName(fileUrl);
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName()).object(objectName).build());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Minio Failed to delete file", e);
        }
    }

    /** 把资源 key 还原成 MinIO 的 objectName（不带前导 /） */
    private String toObjectName(String fileUrl)
    {
        if (!StringUtils.hasText(fileUrl))
        {
            throw new IllegalArgumentException("文件地址为空，不允许删除");
        }
        String path = fileUrl.trim();
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

        String prefix = keyPrefix() + "/";
        if (!path.startsWith(prefix))
        {
            throw new IllegalArgumentException(StringUtils.format("资源文件({})非法，不允许删除。 ", fileUrl));
        }
        String objectName = path.substring(prefix.length());
        if (!StringUtils.hasText(objectName) || objectName.contains(".."))
        {
            throw new IllegalArgumentException(StringUtils.format("资源文件({})非法，不允许删除。 ", fileUrl));
        }
        return objectName;
    }

    /** 规整配置前缀：保证以 / 开头、不以 / 结尾 */
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
