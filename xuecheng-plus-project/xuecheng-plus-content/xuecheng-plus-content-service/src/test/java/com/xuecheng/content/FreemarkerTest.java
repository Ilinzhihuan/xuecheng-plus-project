package com.xuecheng.content;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

@SpringBootTest
public class FreemarkerTest {

    @Resource
    private CoursePublishService coursePublishService;

    @Test
    void testGenerateHtml() throws IOException, TemplateException {
        Configuration configuration = new Configuration(Configuration.getVersion());
        // 获取classpath
        String classpath = this.getClass().getResource("/").getPath();
        // 指定模板目录
        configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
        configuration.setDefaultEncoding("utf-8");
        Template template = configuration.getTemplate("course_template.ftl");
        CoursePreviewDto coursePreviewDto = coursePublishService.getCoursePreviewInfo(125L);
        HashMap<String, Object> map = new HashMap<>();
        map.put("model", coursePreviewDto);
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);

        // 将html写入文件
        InputStream inputStream = IOUtils.toInputStream(html, "UTF-8");
        FileOutputStream outputStream = new FileOutputStream(new File("D:\\IDEACODE\\upload\\125.html"));
        IOUtils.copy(inputStream, outputStream);
    }
}
