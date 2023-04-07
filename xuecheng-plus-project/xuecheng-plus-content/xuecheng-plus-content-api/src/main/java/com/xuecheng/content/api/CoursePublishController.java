package com.xuecheng.content.api;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.List;

/**
 * 课程发布相关接口
 */
@Slf4j
@Controller
public class CoursePublishController {

    @Resource
    private CoursePublishService coursePublishService;

    @Resource
    private TeachplanService teachplanService;

    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView previewf(@PathVariable("courseId") Long courseId) {
        ModelAndView modelAndView = new ModelAndView();

        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);
        // 指定模型
        modelAndView.addObject("model", coursePreviewInfo);
        modelAndView.setViewName("course_template");

        return modelAndView;
    }

    @ResponseBody
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId") Long courseId) {
        Long companyId = 1232141425L;
        coursePublishService.commitAudit(companyId, courseId);
    }

    @ApiOperation("课程发布")
    @ResponseBody
    @PostMapping("/coursepublish/{courseId}")
    public void coursepublish(@PathVariable("courseId") Long courseId) {
        Long companyId = 1232141425L;
        coursePublishService.publish(companyId, courseId);
    }

    @ApiOperation("获取课程发布信息")
    @ResponseBody
    @GetMapping("/course/whole/{courseId}")
    public CoursePreviewDto getCoursePublish(@PathVariable("courseId") Long courseId) {
        // 查询课程发布表
        CoursePublish coursePublish = coursePublishService.getCoursePublish(courseId);
        if (coursePublish == null) {
            log.error("没有该课程的发布信息 courseId:{}", courseId);
            return null;
        }
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();

        // 课程基本信息
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(coursePublish, courseBaseInfoDto);
        coursePreviewDto.setCourseBase(courseBaseInfoDto);
        // 教学计划
        String teachplanStr = coursePublish.getTeachplan();
        if (StringUtils.isBlank(teachplanStr)) {
            log.error("课程id，教学计划为空");
        }
        List<TeachplanDto> teachplanDtos = JSON.parseArray(teachplanStr, TeachplanDto.class);
        coursePreviewDto.setTeachplans(teachplanDtos);
        return coursePreviewDto;
    }
}