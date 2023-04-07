package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FreemarkerController {

    @GetMapping("/testfreemarker")
    public ModelAndView test() {
        ModelAndView modelAndView = new ModelAndView();
        // 指定模型
        modelAndView.addObject("name", "linzhihuan");
        // 指定模板
        modelAndView.setViewName("test");
        return modelAndView;
    }

}
