package cn.javayong.platform.sms.admin.dispatcher.processor.requeset;

/**
 * 请求命令枚举
 * Created by zhangyong on 2023/7/14.
 */
public class RequestCode {

    public static final int CREATE_SMS_RECORD_MESSAGE = 10;

    public static final int APPLY_TEMPLATE = 11;

    // 立即发送短信
    public static final int NOW_SEND_MESSAGE = 12;

    // 延迟短信发送
    public static final int DELAY_SEND_MESSAGE = 13;

}
