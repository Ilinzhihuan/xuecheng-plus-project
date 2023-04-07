package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignClient.MediaFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;


@SpringBootTest
public class FeignUploadTest {

    @Resource
    private MediaFeignClient mediaFeignClient;

    @Test
    void test() throws IOException {
        File file = new File("D:\\IDEACODE\\upload\\125.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        mediaFeignClient.upload(multipartFile, "course/125.html");
    }
}
