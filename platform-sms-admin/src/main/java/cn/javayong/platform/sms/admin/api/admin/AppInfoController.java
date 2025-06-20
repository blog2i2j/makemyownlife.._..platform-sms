package cn.javayong.platform.sms.admin.api.admin;

import cn.javayong.platform.sms.admin.common.utils.ResponseEntity;
import cn.javayong.platform.sms.admin.domain.TSmsAppinfo;
import cn.javayong.platform.sms.admin.domain.vo.Pager;
import cn.javayong.platform.sms.admin.domain.vo.RecordVO;
import cn.javayong.platform.sms.admin.service.AppInfoService;
import cn.javayong.platform.sms.admin.service.SmsRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sms")
public class AppInfoController {

    private final static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private AppInfoService appInfoService;

    @Autowired
    private SmsRecordService smsRecordService;

    @PostMapping(value = "/appList")
    public ResponseEntity<Pager> appList(String appkey, int page, int size) {
        Map<String, Object> param = new HashMap<>();
        param.put("appkey", appkey);
        param.put("page", page);
        param.put("size", size);
        List<TSmsAppinfo> appinfoList = appInfoService.selectAppInfoListPage(param);
        Integer count = appInfoService.selectAppInfoCount(param);
        Pager pager = new Pager();
        pager.setCount(Long.valueOf(count));
        pager.setItems(appinfoList);
        return ResponseEntity.success(pager);
    }

    @PostMapping(value = "/addAppInfo")
    public ResponseEntity addAppInfo(@RequestBody TSmsAppinfo appinfo) {
        return appInfoService.addAppInfo(appinfo);
    }

    @PostMapping(value = "/updateAppInfo")
    public ResponseEntity updateAppInfo(@RequestBody TSmsAppinfo appinfo) {
        return appInfoService.updateAppInfo(appinfo);
    }

    @PostMapping(value = "/deleteAppInfo")
    public ResponseEntity deleteAppInfo(String id) {
        if (Long.valueOf(id) == 1) {
            logger.info("默认应用不能删除");
            return ResponseEntity.fail("默认应用不能删除");
        }
        //判断应用是否有短信
        List<RecordVO> recordVOList = smsRecordService.queryOneRecordVOByAppId(id);
        if (!CollectionUtils.isEmpty(recordVOList)) {
            logger.info("本应用曾发送过短信，不能删除");
            return ResponseEntity.fail("本应用曾发送过短信，不能删除");
        }
        return appInfoService.deleteAppInfo(id);
    }


}
