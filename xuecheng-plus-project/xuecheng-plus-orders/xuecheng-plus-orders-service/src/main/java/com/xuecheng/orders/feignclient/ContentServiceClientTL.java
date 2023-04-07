package com.xuecheng.orders.feignclient;

import com.xuecheng.orders.model.dto.CourseBaseInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/content")
@FeignClient(value = "content-api",fallbackFactory = ContentServiceClientTLFallbackFactory.class)
public interface ContentServiceClientTL {

    @GetMapping("/course/{courseId}")
    CourseBaseInfoDto getCourseBaseById(@PathVariable(value = "courseId") Long courseId);

    @GetMapping("/r/course/{courseId}")
    String getCourseName(@PathVariable("courseId") Long courseId);
}
