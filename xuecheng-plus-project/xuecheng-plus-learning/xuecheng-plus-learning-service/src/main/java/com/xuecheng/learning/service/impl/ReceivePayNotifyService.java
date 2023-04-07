package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class ReceivePayNotifyService {

    @Resource
    private MyCourseTablesServiceImpl myCourseTablesService;

    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] body = message.getBody();
        String jsonStr = new String(body);
        // 转换为对象
        MqMessage mqMessage = JSON.parseObject(jsonStr, MqMessage.class);
        // 解析消息
        // 选课id
        String chooseCourseId = mqMessage.getBusinessKey1();
        // 订单类型
        String orderType = mqMessage.getBusinessKey2();
        if (orderType.equals("60201")) {
            // 根据消息内容 更新选课记录 写入我的课程表
            boolean isSuccess = myCourseTablesService.saveChooseCourseSuccess(chooseCourseId);
            if (!isSuccess) {
                XueChengPlusException.cast("保存选课记录状态失败");
            }
        }
    }

}
