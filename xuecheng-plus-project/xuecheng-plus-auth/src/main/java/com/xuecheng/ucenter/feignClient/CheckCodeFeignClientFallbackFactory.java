package com.xuecheng.ucenter.feignClient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CheckCodeFeignClientFallbackFactory implements FallbackFactory<CheckCodeFeignClient> {
    @Override
    public CheckCodeFeignClient create(Throwable throwable) {
        return (key, code) -> {
            log.debug("调用验证码服务熔断异常:{}", throwable.getMessage());
            return null;
        };
    }
}
