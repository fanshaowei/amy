package com.amy.common.core.utils.poi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.util.DefaultTempFileCreationStrategy;
import org.apache.poi.util.TempFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POI 临时目录初始化。
 *
 * <p>问题背景：POI 4.1.2 的 {@code SXSSFWorkbook} 默认把临时文件写到
 * {@code ${java.io.tmpdir}/poifiles}。生产上这个目录经常因为「之前用 root 启动过」
 * 或容器只读挂载导致当前启动用户不可写，构造 workbook 时直接抛
 * {@code java.io.IOException: Permission denied}，所有导出失败；而本地该目录可写，
 * 即使连的是同一个生产库也能成功——差异只在服务所在机器的本地磁盘权限。</p>
 *
 * <p>解决：把 POI 的临时文件策略全局切换到应用自有目录，不再依赖系统 tmpdir 权限。
 * 目录选择顺序：</p>
 * <ol>
 *   <li>JVM 参数 {@code -Damy.poi.temp.dir=/path}（部署时按需覆盖）</li>
 *   <li>应用工作目录下的 {@code temp/poi}</li>
 *   <li>系统临时目录下的 {@code poifiles}（POI 默认位置，兜底）</li>
 * </ol>
 * <p>每个候选目录都会真实创建一个探针文件验证可写，仅 {@code mkdirs} 成功不算数
 * （生产故障恰恰是「目录存在但不可写」）。</p>
 *
 * <p>本类是幂等的，重复调用 {@link #install()} 只会生效一次。</p>
 */
public final class PoiTempDir
{
    private static final Logger log = LoggerFactory.getLogger(PoiTempDir.class);

    /** 可通过 JVM 参数覆盖，例如 -Damy.poi.temp.dir=/data/amy/temp/poi */
    public static final String TEMP_DIR_PROPERTY = "amy.poi.temp.dir";

    private static final String DEFAULT_SUB_DIR = "temp/poi";

    private static final String FALLBACK_SUB_DIR = "poifiles";

    private static volatile boolean installed = false;

    private PoiTempDir()
    {
    }

    /**
     * 注册全局临时文件策略（幂等）。
     */
    public static synchronized void install()
    {
        if (installed)
        {
            return;
        }
        File dir = resolveWritableDir();
        TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy(dir));
        installed = true;
        log.info("POI 临时目录已切换为应用自有目录: {}", dir.getAbsolutePath());
    }

    /**
     * 按优先级选出第一个可写目录；全部不可写时抛出带明确提示的异常。
     */
    private static File resolveWritableDir()
    {
        List<File> candidates = new ArrayList<File>();

        String configured = System.getProperty(TEMP_DIR_PROPERTY);
        if (configured != null && !configured.trim().isEmpty())
        {
            candidates.add(new File(configured.trim()));
        }

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.trim().isEmpty())
        {
            candidates.add(new File(userDir, DEFAULT_SUB_DIR));
        }

        String javaTmpDir = System.getProperty("java.io.tmpdir");
        if (javaTmpDir != null && !javaTmpDir.trim().isEmpty())
        {
            candidates.add(new File(javaTmpDir, FALLBACK_SUB_DIR));
        }

        IOException lastError = null;
        for (File dir : candidates)
        {
            try
            {
                ensureWritable(dir);
                return dir;
            }
            catch (IOException e)
            {
                lastError = e;
                log.warn("POI 临时目录不可用，尝试下一个: {} ({})", dir.getAbsolutePath(), e.getMessage());
            }
        }

        throw new IllegalStateException("无法找到可写的 POI 临时目录，请通过 JVM 参数 -D"
                + TEMP_DIR_PROPERTY + "=/可写路径 指定", lastError);
    }

    /**
     * 真实创建一个探针文件来验证目录可写（目录存在不等于可写）。
     */
    private static void ensureWritable(File dir) throws IOException
    {
        if (dir.exists())
        {
            if (!dir.isDirectory())
            {
                throw new IOException("路径不是目录: " + dir.getAbsolutePath());
            }
        }
        else if (!dir.mkdirs() && !dir.isDirectory())
        {
            throw new IOException("创建目录失败: " + dir.getAbsolutePath());
        }

        File probe = File.createTempFile("poi_write_probe_", ".tmp", dir);
        try
        {
            // 能创建即证明可写
        }
        finally
        {
            if (!probe.delete())
            {
                probe.deleteOnExit();
            }
        }
    }
}
