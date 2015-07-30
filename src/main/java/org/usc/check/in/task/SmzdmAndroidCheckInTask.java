package org.usc.check.in.task;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.usc.check.in.model.Account;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 *
 * @author Shunli
 */
@Component
public class SmzdmAndroidCheckInTask extends BaseTask {
    private static final Logger log = LoggerFactory.getLogger(SmzdmAndroidCheckInTask.class);

    private static final String LOGIN_URL = "https://api.smzdm.com/v1/user/login";
    private static final String CHECK_IN_URL = "https://api.smzdm.com/v1/user/checkin";

    private static final String SSID = "D8abqe1FRAD2q037uVrvioeMW1Wbc4FV";
    private static final String USER_AGENT = "smzdm_android_V5.6.1 rv:240 (MI 4LTE;Android4.4.4;zh)";

    @Override
    protected String name() {
        return "smzdm";
    }

    @Scheduled(cron = "0 0 5,18 * * ?")
    public void run() {
        for (Account account : buildAccounts()) {
            try {
                Executor executor = Executor.newInstance().cookieStore(new BasicCookieStore());
                String token = login(executor, account);
                if (StringUtils.isNotEmpty(token)) {
                    checkIn(executor, account, token);
                }
            } catch (Exception e) {
                log.error("【SMZDM】【" + account.getUsername() + "】签到异常", e);
            }
        }
    }

    private String login(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsername();

        List<NameValuePair> formParams = buildFormParams("");
        formParams.add(new BasicNameValuePair("user_login", usrename));
        formParams.add(new BasicNameValuePair("user_pass", account.getPassword()));

        String loginJson = executor.execute(appendTimeOuts(Request.Post(LOGIN_URL)).bodyForm(formParams).userAgent(USER_AGENT)).returnContent().asString();

        JSONObject loginJsonParseObject = JSON.parseObject(loginJson);
        if (0 != loginJsonParseObject.getInteger("error_code")) {
            log.info("【SMZDM】【{}】登录失败：{}", usrename, loginJsonParseObject.getString("error_msg"));
            return StringUtils.EMPTY;
        }

        log.info("【SMZDM】【{}】登录成功", usrename);
        return loginJsonParseObject.getJSONObject("data").getString("token");
    }

    private boolean checkIn(Executor executor, Account account, String token) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsername();

        String checkInJson = executor.execute(appendTimeOuts(Request.Post(CHECK_IN_URL)).bodyForm(buildFormParams(token)).userAgent(USER_AGENT)).returnContent().asString();

        JSONObject checkInParseObject = JSON.parseObject(checkInJson);
        if (0 != checkInParseObject.getInteger("error_code")) {
            log.info("【SMZDM】【{}】签到失败：{}", usrename, checkInParseObject.getString("error_msg"));
            return false;
        }

        log.info("【SMZDM】【{}】签到成功", usrename);
        return true;
    }
    private static List<NameValuePair> buildFormParams(String token) {
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("captcha", ""));
        formParams.add(new BasicNameValuePair("f", "android"));
        formParams.add(new BasicNameValuePair("s", SSID));
        formParams.add(new BasicNameValuePair("token", token));
        formParams.add(new BasicNameValuePair("partner_id", "3"));
        formParams.add(new BasicNameValuePair("v", "240"));
        formParams.add(new BasicNameValuePair("", ""));
        return formParams;
    }

}
