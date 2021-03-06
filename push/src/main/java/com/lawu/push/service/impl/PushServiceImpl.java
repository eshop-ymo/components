package com.lawu.push.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.gexin.rp.sdk.base.IPushResult;
import com.gexin.rp.sdk.base.impl.AppMessage;
import com.gexin.rp.sdk.base.impl.SingleMessage;
import com.gexin.rp.sdk.base.impl.Target;
import com.gexin.rp.sdk.exceptions.RequestException;
import com.gexin.rp.sdk.http.IGtPush;
import com.gexin.rp.sdk.template.TransmissionTemplate;
import com.lawu.push.service.GtPushService;

/**
 * @author zhangyong
 * @date 2018/1/31.
 */
public class PushServiceImpl implements GtPushService, InitializingBean {

    private static Logger logger = LoggerFactory.getLogger(PushServiceImpl.class);
    /**
     * 毫秒 24小时  24 * 3600 * 1000
     */
    private static final long OFFLINE_EXPIRE_TIME = 86400000L;

    private static final String RESULT = "result";

    private String gtHost;

    private String appId;

    private String appKey;

    private String masterSecret;

    private TransmissionTemplate template;

    private IGtPush push;

    private Boolean isClientId;

    @Override
    public String pushMessageToSingle(String cid, String title, String contents) {
        SingleMessage message = new SingleMessage();
        message.setData(PushCommonClient.getTemplate(template, title, contents));
        message.setOffline(true);
        // 离线有效时间，单位为毫秒，可选
        message.setOfflineExpireTime(OFFLINE_EXPIRE_TIME);
        // 可选，1为wifi，0为不限制网络环境。根据手机处于的网络情况，决定是否下发
        message.setPushNetWorkType(0);
        Target target = new Target();
        if(isClientId){
            target.setClientId(cid);
        }else{
            target.setAlias(cid);
        }
        target.setAppId(appId);
        IPushResult ret;
        try {
            ret = push.pushMessageToSingle(message, target);
            if (ret != null) {
                String result = (String) ret.getResponse().get(RESULT);
                logger.info("gtPush result:result{}", result);
                return result;
            } else {
                logger.info("gtPush 推送失败:{}", "gtPush--服务器响应异常");
                return "false";
            }
        } catch (RequestException e) {
            logger.info("gtPush--服务器响应异常，{}", e);
            return "false";
        }
    }

    @Override
    public String pushMessageToAll(String title, String contents) {
        AppMessage message = new AppMessage();
        message.setOffline(true);
        // 离线有效时间，单位为毫秒，可选
        message.setOfflineExpireTime(OFFLINE_EXPIRE_TIME);
        // 推送给App的目标用户需要满足的条件
        message.setData(PushCommonClient.getTemplate(template, title, contents));
        List<String> appIdList = new ArrayList<>();
        appIdList.add(appId);
        message.setAppIdList(appIdList);
        IPushResult ret = push.pushMessageToApp(message);
        String result = (String) ret.getResponse().get(RESULT);
        logger.info("push-all-member result {}", result);
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        template = new TransmissionTemplate();
        template.setAppId(appId);
        template.setAppkey(appKey);
        template.setTransmissionType(2);

        push = new IGtPush(gtHost, appKey, masterSecret);
    }

    public void setGtHost(String gtHost) {
        this.gtHost = gtHost;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public void setMasterSecret(String masterSecret) {
        this.masterSecret = masterSecret;
    }

    public void setIsClientId(Boolean clientId) {
        isClientId = clientId;
    }
}
