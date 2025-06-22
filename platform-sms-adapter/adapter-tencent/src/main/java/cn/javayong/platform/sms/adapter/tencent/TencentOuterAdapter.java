package cn.javayong.platform.sms.adapter.tencent;

import cn.javayong.platform.sms.adapter.support.SmsChannelConfig;
import cn.javayong.platform.sms.adapter.support.SmsTemplateUtil;
import com.alibaba.fastjson.JSON;
import cn.javayong.platform.sms.adapter.OuterAdapter;
import cn.javayong.platform.sms.adapter.command.req.AddSmsTemplateReqCommand;
import cn.javayong.platform.sms.adapter.command.req.QuerySmsTemplateReqCommand;
import cn.javayong.platform.sms.adapter.command.req.SendSmsReqCommand;
import cn.javayong.platform.sms.adapter.command.resp.SmsRespCommand;
import cn.javayong.platform.sms.adapter.support.SPI;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SPI("tencent")
public class TencentOuterAdapter implements OuterAdapter {

    private final static Logger logger = LoggerFactory.getLogger(TencentOuterAdapter.class);

    private String instanceId = UUID.randomUUID().toString().replaceAll("-", "");

    private SmsChannelConfig smsChannelConfig;

    private SmsClient client;

    private String appId;

    @Override
    public void init(SmsChannelConfig smsChannelConfig) {
        logger.info("初始化腾讯短信客户端 渠道编号:[" + smsChannelConfig.getId() + "] appkey:[" + smsChannelConfig.getChannelAppkey() + "] 实例Id:" + instanceId);
        this.smsChannelConfig = smsChannelConfig;
        this.appId = smsChannelConfig.getExtProperties();
        // SecretId、SecretKey 查询: https://console.cloud.tencent.com/cam/capi
        Credential cred = new Credential(smsChannelConfig.getChannelAppkey(), smsChannelConfig.getChannelAppsecret());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setReqMethod("POST");
        httpProfile.setConnTimeout(60);
        httpProfile.setEndpoint("sms.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setSignMethod("HmacSHA256");
        clientProfile.setHttpProfile(httpProfile);
        this.client = new SmsClient(cred, "ap-guangzhou", clientProfile);
    }

    @Override
    public SmsRespCommand<String> sendSmsByTemplateId(SendSmsReqCommand sendSmsReqCommand) {
        // 参考腾讯文档 ：https://cloud.tencent.com/document/product/382/43194
        try {
            SendSmsRequest sendSmsRequest = new SendSmsRequest();
            sendSmsRequest.setSmsSdkAppId(appId);
            sendSmsRequest.setSignName(sendSmsReqCommand.getSignName());
            sendSmsRequest.setTemplateId(sendSmsReqCommand.getTemplateCode());
            // 模板参数的个数需要与 TemplateId 对应模板的变量个数保持一致，若无模板参数，则设置为空
            sendSmsRequest.setTemplateParamSet(SmsTemplateUtil.renderTemplateParamArray(sendSmsReqCommand.getTemplateParam(), sendSmsReqCommand.getTemplateContent()));
            sendSmsRequest.setPhoneNumberSet(new String[]{"+86" + sendSmsReqCommand.getPhoneNumbers()});
            sendSmsRequest.setExtendCode(StringUtils.EMPTY);
            sendSmsRequest.setSenderId(StringUtils.EMPTY);
            logger.info("tencent send sms:" + JSON.toJSONString(sendSmsRequest));
            SendSmsResponse response = client.SendSms(sendSmsRequest);
            logger.info("response:" + JSON.toJSONString(response));
            SendStatus[] sendStatuArray = response.getSendStatusSet();
            if (sendStatuArray != null && sendStatuArray.length > 0) {
                SendStatus sendStatus = sendStatuArray[0];
                if ("Ok".equals(sendStatus.getCode())) {
                    return new SmsRespCommand(SmsRespCommand.SUCCESS_CODE, sendStatus.getSerialNo());
                } else {
                    return new SmsRespCommand(SmsRespCommand.FAIL_CODE, null, sendStatus.getMessage());
                }
            }
            return new SmsRespCommand(SmsRespCommand.FAIL_CODE);
        } catch (Exception e) {
            logger.error("tencent sendSms error:", e);
            return new SmsRespCommand(SmsRespCommand.FAIL_CODE);
        }
    }

    @Override
    public SmsRespCommand<Map<String, String>> addSmsTemplate(AddSmsTemplateReqCommand addSmsTemplateReqCommand) {
        try {
            AddSmsTemplateRequest addSmsTemplateRequest = new AddSmsTemplateRequest();
            addSmsTemplateRequest.setSmsType(Long.valueOf(addSmsTemplateReqCommand.getTemplateType()));
            addSmsTemplateRequest.setTemplateName(addSmsTemplateReqCommand.getTemplateName());
            // 腾讯云：{1}为您的登录验证码，请于{2}分钟内填写，如非本人操作，请忽略本短信。
            // 标准版：${code}为您的登录验证码，请于${time}分钟内填写，如非本人操作，请忽略本短信。
            String templateContent = SmsTemplateUtil.replaceStandardToTencent(addSmsTemplateReqCommand.getTemplateContent());
            addSmsTemplateRequest.setTemplateContent(templateContent);
            addSmsTemplateRequest.setRemark(addSmsTemplateReqCommand.getRemark());
            logger.info("tencent addSmsTemplateRequest:" + JSON.toJSONString(addSmsTemplateRequest));
            AddSmsTemplateResponse addSmsTemplateResponse = client.AddSmsTemplate(addSmsTemplateRequest);
            logger.info("tencent addSmsTemplateResponse:" + JSON.toJSONString(addSmsTemplateResponse));
            if (addSmsTemplateResponse != null) {
                AddTemplateStatus addTemplateStatus = addSmsTemplateResponse.getAddTemplateStatus();
                if (addTemplateStatus != null) {
                    Map<String, String> bodyMap = new HashMap<>();
                    bodyMap.put("templateCode", addTemplateStatus.getTemplateId());
                    bodyMap.put("templateContent", templateContent);
                    return new SmsRespCommand(SmsRespCommand.SUCCESS_CODE, bodyMap);
                }
            }
        } catch (Exception e) {
            logger.error("tencent addSmsTemplate error: ", e);
        }
        return new SmsRespCommand(SmsRespCommand.FAIL_CODE);
    }

    @Override
    public SmsRespCommand<Integer> querySmsTemplateStatus(QuerySmsTemplateReqCommand querySmsTemplateReqCommand) {
        try {
            DescribeSmsTemplateListRequest describeSmsTemplateListRequest = new DescribeSmsTemplateListRequest();
            describeSmsTemplateListRequest.setTemplateIdSet(new Long[]{Long.valueOf(querySmsTemplateReqCommand.getTemplateCode())});
            describeSmsTemplateListRequest.setInternational(0L);
            DescribeSmsTemplateListResponse response = client.DescribeSmsTemplateList(describeSmsTemplateListRequest);
            if (response != null) {
                DescribeTemplateListStatus[] arr = response.getDescribeTemplateStatusSet();
                if (arr == null) {
                    logger.error("tencent templateId:" + querySmsTemplateReqCommand.getTemplateCode() + " 不存在");
                } else {
                    Long statusCode = arr[0].getStatusCode();
                    //腾讯:申请模板状态，其中0表示审核通过且已生效，1表示审核中，2表示审核通过待生效，-1表示审核未通过或审核失败。注：只有状态值为0时该模板才能使用。
                    //标准: 0 : 待提交 1：待审核  2：审核成功 3：审核失败
                    if (statusCode == 0L) {
                        return new SmsRespCommand(SmsRespCommand.SUCCESS_CODE, 2);
                    }
                    if (statusCode == 1L) {
                        return new SmsRespCommand(SmsRespCommand.SUCCESS_CODE, 1);
                    }
                    if (statusCode == -1L) {
                        return new SmsRespCommand(SmsRespCommand.SUCCESS_CODE, 3);
                    }
                    if (statusCode == 2L) {
                        return new SmsRespCommand(SmsRespCommand.SUCCESS_CODE, 1);
                    }
                }
            }
            return new SmsRespCommand(SmsRespCommand.FAIL_CODE);
        } catch (Exception e) {
            logger.error("tencent addSmsTemplate error: ", e);
            return new SmsRespCommand(SmsRespCommand.FAIL_CODE, e.getMessage());
        }
    }

    @Override
    public void destroy() {
        logger.warn("销毁腾讯短信客户端 渠道编号:[" + smsChannelConfig.getId() + "] appkey:[" + smsChannelConfig.getChannelAppkey() + "] 实例Id:" + instanceId);
    }

}
