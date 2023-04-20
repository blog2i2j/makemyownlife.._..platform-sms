package com.courage.platform.sms.server.controller;

import com.courage.platform.sms.client.SmsSenderResult;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/api")
public class SenderController {

    private final static Logger logger = LoggerFactory.getLogger(SenderController.class);

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.topic}")
    private String smsTopic;

    @RequestMapping("/sendByTemplateId")
    @ResponseBody
    public SmsSenderResult sendByTemplateId(HttpServletRequest request) {
        try {
            //接收请求参数
            String q = request.getParameter("q");
            String appKey = request.getParameter("appKey");
            String time = request.getParameter("time");
            String random = request.getParameter("random");
            //构造唯一请求id
            String uniqueId = time + random;
            logger.info("q:" + q + " appKey:" + appKey + " uniqueId:" + uniqueId);
            //发送消息
            String tag = "template";
            SendResult sendResult = rocketMQTemplate.syncSend(
                    smsTopic + ":" + tag,
                    //链式调用
                    MessageBuilder.withPayload(q).setHeader(MessageConst.PROPERTY_KEYS, uniqueId).build());
            logger.info("uniqueId：" + uniqueId + " sendResult:" + sendResult);
            if (sendResult != null) {
                if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
                    return new SmsSenderResult(sendResult.getMsgId());
                }
            }
            return new SmsSenderResult(SmsSenderResult.FAIL_CODE, "发送失败");
        } catch (Exception e) {
            logger.error("sendSingle error: ", e);
            return new SmsSenderResult(SmsSenderResult.FAIL_CODE, "发送失败");
        }
    }

}
