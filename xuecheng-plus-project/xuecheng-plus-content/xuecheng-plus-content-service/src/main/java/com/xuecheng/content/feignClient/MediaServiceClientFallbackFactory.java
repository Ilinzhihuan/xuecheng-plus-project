package com.xuecheng.content.feignClient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaFeignClient> {
    @Override
    public MediaFeignClient create(Throwable throwable) {

        // 发生熔断 上游服务调用此方法执行降级逻辑
        return (filedata, objectName) -> {
            log.error("远程调用上传文件的接口发生熔断:{}", throwable.toString(), throwable);
            return null;
        };
    }
}
