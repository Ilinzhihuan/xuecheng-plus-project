package com.xuecheng.content.service.jobHandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignClient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.po.CourseIndex;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;


@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Resource
    private CoursePublishService coursePublishService;

    @Resource
    private SearchServiceClient searchServiceClient;

    @Resource
    private CoursePublishMapper coursePublishMapper;
    // 任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        // 执行任务
        process(shardIndex, shardTotal, "course_publish", 30, 60);

    }

    // 执行课程发布任务的逻辑
    @Override
    public boolean execute(MqMessage mqMessage) {
        // 获取课程id
        long courseId = Long.parseLong(mqMessage.getBusinessKey1());
        // 课程静态化上传minio
        generateCourseHtml(mqMessage, courseId);
        // 向es写索引数据
        saveCourseIndex(mqMessage, courseId);
//        // redis写入缓存
//        saveCourseCache(mqMessage, courseId);
        return true;
    }

    //生成课程静态化页面并上传至文件系统
    public void generateCourseHtml(MqMessage mqMessage,long courseId){
        // 消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        // 幂等性处理
        int stageOne = mqMessageService.getStageOne(taskId);
        if (stageOne > 0) {
            log.debug("课程静态化页面任务完成 无需处理");
            return;
        }
        // 开始静态化页面处理 生成html
        File file = coursePublishService.generateCourseHtml(courseId);
        if (file == null) {
            XueChengPlusException.cast("生成的静态页面为空");
        }
        // 将html文件上传到minio
        coursePublishService.uploadCourseHtml(courseId, file);

        mqMessageService.completedStageOne(taskId);
    }

    //保存课程索引信息
    public void saveCourseIndex(MqMessage mqMessage,long courseId){
        // 消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        // 幂等性处理
        int stageTwo = mqMessageService.getStageTwo(taskId);
        if (stageTwo > 0) {
            log.debug("课程索引信息任务完成 无需处理");
            return;
        }
        // 查询课程信息 调用搜索服务添加索引
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish, courseIndex);
        // 远程调用
        Boolean isAdd = searchServiceClient.add(courseIndex);
        if (!isAdd) {
            XueChengPlusException.cast("远程调用搜索服务添加课程索引失败");
        }
        // 保存课程索引信息
        mqMessageService.completedStageTwo(taskId);
    }


    //将课程信息缓存至redis
    public void saveCourseCache(MqMessage mqMessage,long courseId){
        log.debug("将课程信息缓存至redis,课程id:{}",courseId);
        // 消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        // 幂等性处理
        int stageThree = mqMessageService.getStageThree(taskId);
        if (stageThree > 0) {
            log.debug("将课程信息缓存至redis 无需处理");
            return;
        }
        // 保存课程索引信息
        mqMessageService.completedStageThree(taskId);
    }
}
