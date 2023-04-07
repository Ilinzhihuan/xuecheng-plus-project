package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * XxlJob开发示例（Bean模式）
 * 开发步骤：
 * 1、任务开发：在Spring Bean实例中，开发Job方法；
 * 2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 * 4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Slf4j
@Component
public class VideoTask {
    private static Logger logger = LoggerFactory.getLogger(VideoTask.class);

    @Resource
    private MediaFileProcessService mediaFileProcessService;

    @Resource
    private MediaFileService mediaFileService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegPath;

    @Resource
    private MinioClient minioClient;


    /**
     * 2、分片广播任务
     */
    @XxlJob("videoJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("执行器的序号:{}", shardIndex);
        log.info("执行器总数:{}", shardTotal);

        int processors = Runtime.getRuntime().availableProcessors();

        // 查询待处理任务
        List<MediaProcess> mediaProcessList =
                mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        int size = mediaProcessList.size();
        if (size == 0) {
            log.error("视频处理任务数: " + size);
            return;
        }

        // 创建一个线程池
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        // 计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            // 将任务加入线程池
            executorService.execute(() -> {
                try {
                    // 任务id
                    Long taskId = mediaProcess.getId();
                    // 开启任务
                    if (!mediaFileProcessService.startTask(taskId)) {
                        log.error("抢占任务失败:{}", taskId);
                        return;
                    }
                    // 文件id MD5值
                    String fileId = mediaProcess.getFileId();
                    // 执行视频转码
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();
                    // 下载minio视频
                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (file == null) {
                        log.error("文件下载失败, 任务id:{}, bucket:{} objectName:{}", taskId, bucket, objectName);
                        // 保存错误信息
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "下载视频到本地失败");
                        return;
                    }
                    // 上传minio

                    // 源avi文件视频的路径
                    String video_path = file.getAbsolutePath();
                    // 转换后MP4文件的名称
                    String mp4_name = fileId + ".mp4";

                    // 创建一个临时文件作为转换后的文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.error("创建临时文件异常:{}", e.getMessage());
                        // 保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "创建临时文件异常");
                        return;
                    }
                    // 转换后MP4文件的路径
                    String mp4_path = mp4File.getAbsolutePath();
                    //String md5 = DigestUtils.md5Hex(Files.newInputStream(mp4File.toPath()));
                    String mp4_objectName = getFilePath(fileId);
                    // 创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegPath, video_path, mp4_name, mp4_path);
                    // 开始视频转换
                    String result = videoUtil.generateMp4();
                    if (!"success".equals(result)) {
                        log.error("文件转换失败:{} bucket:{}, objectName:{}", result, bucket, mp4_objectName);
                        // 保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, result);
                        return;
                    }

                    // 上传到minio

                    boolean isSuccess = mediaFileService.addMediaFilesToMinIO(mp4_path, "video/mp4", bucket, mp4_objectName);

                    if (!isSuccess) {
                        log.error("mp4文件上传到minio失败 taskId:{}", taskId);
                        // 保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "mp4文件上传到minio失败");
                        return;
                    }
                    // MP4文件的url
                    String url = "/" + bucket + "/" + getFilePath(fileId);
                    // 成功
                    // 删除原来minio中的avi文件
                    boolean delete = clearAviFiles(objectName, bucket);
                    if (!delete) {
                        log.error("avi文件删除失败");
                    }
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, "视频转换成功");
                } finally {
                    countDownLatch.countDown();
                }
            });
        });
        countDownLatch.await(30, TimeUnit.MINUTES);


    }

    /**
     * 清除avi文件
     *
     * @param aviPath avi路径
     * @param bucket  桶
     */
    private boolean clearAviFiles(String aviPath, String bucket) {
        //RemoveObjectArgs
        RemoveObjectArgs removeObjectArgs =
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(aviPath)
                        .build();
        //删除文件
        try {
            minioClient.removeObject(removeObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String getFilePath(String fileMd5) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + ".mp4";
    }

}
