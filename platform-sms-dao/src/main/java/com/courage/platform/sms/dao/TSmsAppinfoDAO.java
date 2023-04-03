package com.courage.platform.sms.dao;

import com.courage.platform.sms.domain.TSmsAppinfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface TSmsAppinfoDAO extends MyBatisBaseDao<TSmsAppinfo, Long> {

    TSmsAppinfo getAppinfoByAppKey(@Param("appKey") String appKey);

    List<TSmsAppinfo> selectAll();

    List<TSmsAppinfo> selectAppInfoListPage(Map<String, Object> param);

    Integer selectAppInfoCount(Map<String, Object> param);

}