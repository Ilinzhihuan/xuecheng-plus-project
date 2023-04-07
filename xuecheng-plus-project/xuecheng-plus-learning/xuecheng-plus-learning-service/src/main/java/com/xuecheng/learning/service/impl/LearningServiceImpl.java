package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.spring.util.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class LearningServiceImpl implements LearningService {

    @Resource
    private MyCourseTablesService myCourseTablesService;

    @Resource
    private ContentServiceClient contentServiceClient;

    @Resource
    private MediaServiceClient mediaServiceClient;

    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {
        // 查询课程
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            return RestResponse.validfail("课程不存在");
        }
        String teachplanJson = coursepublish.getTeachplan();
        List<TeachplanDto> teachplanDtos = JSON.parseArray(teachplanJson, TeachplanDto.class);
        // 判断是否支持试学
        for (TeachplanDto teachplanDto : teachplanDtos) {
            if (teachplanDto.getId().equals(teachplanId)) {
                return mediaServiceClient.getPlayUrlByMediaId(mediaId);
            }
        }
        // 用户已登录
        if (StringUtils.isNotBlank(userId)) {
            // 课程表
            XcCourseTablesDto learningStatus = myCourseTablesService.getLearningStatus(userId, courseId);
            String learnStatus = learningStatus.getLearnStatus();
            if ("702002".equals(learnStatus)) {
                return RestResponse.validfail("无法学习 没有选课或选课后未支付");
            } else if ("702003".equals(learnStatus)) {
                return RestResponse.validfail("已过期需要申请续期或重新支付");
            } else {
                // 有资格学习 返回视频播放地址
                return mediaServiceClient.getPlayUrlByMediaId(mediaId);
            }
        }
        // 用户未登录
        String charge = coursepublish.getCharge();
        if ("201000".equals(charge)) {
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }
        return RestResponse.validfail("该课程没有选课");
    }

}
