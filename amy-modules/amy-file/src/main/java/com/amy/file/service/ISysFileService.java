package com.amy.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口
 * 
 * @author amy
 */
public interface ISysFileService
{
    /**
     * 文件上传接口
     * 
     * @param file 上传的文件
     * @return 资源 key（不含协议/域名/网关前缀），形如 {@code <file.prefix>/2026/11/26/xxx.jpg}；
     *         前缀只由 Nacos {@code file.prefix} 决定；后端出站原样返回，
     *         访问地址由前端按部署环境自行拼装
     * @throws Exception
     */
    public String uploadFile(MultipartFile file) throws Exception;

    /**
     * 文件删除接口
     * 
     * @param fileUrl 按当前 {@code file.prefix} 生成的资源 key
     * @throws Exception
     */
    public void deleteFile(String fileUrl) throws Exception;
}
