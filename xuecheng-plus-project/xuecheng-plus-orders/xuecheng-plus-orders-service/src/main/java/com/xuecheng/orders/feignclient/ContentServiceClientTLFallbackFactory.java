package com.xuecheng.orders.feignclient;

import com.xuecheng.orders.model.dto.CourseBaseInfoDto;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContentServiceClientTLFallbackFactory implements FallbackFactory<ContentServiceClientTL> {
    @Override
    public ContentServiceClientTL create(Throwable throwable) {
        return new ContentServiceClientTL() {
            @Override
            public CourseBaseInfoDto getCourseBaseById(Long courseId) {
                log.error("调用内容管理服务发生熔断:{}", throwable.toString(), throwable);
                return null;
            }

            @Override
            public String getCourseName(Long courseId) {
                log.error("调用内容管理服务发生熔断:{}", throwable.toString(), throwable);
                return null;
            }

        };
    }
}
