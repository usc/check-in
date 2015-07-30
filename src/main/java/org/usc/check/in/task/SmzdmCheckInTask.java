package org.usc.check.in.task;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.usc.check.in.model.Account;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 *
 * @author Shunli
 */
@Component
public class SmzdmCheckInTask extends BaseTask {
    private static final Logger log = LoggerFactory.getLogger(SmzdmCheckInTask.class);

    private static final String LOGIN_URL = "http://www.smzdm.com/user/login/jsonp_check";
    private static final String CHECK_IN_URL = "http://www.smzdm.com/user/qiandao/jsonp_checkin";

    @Override
    protected String name() {
        return "smzdm";
    }

    // @Scheduled(cron = "0 0 5,18 * * ?")
    public void run() {
        for (Account account : buildAccounts()) {
            try {
                Executor executor = Executor.newInstance().cookieStore(new BasicCookieStore());
                if (login(executor, account)) {
                    checkIn(executor, account);
                }
            } catch (Exception e) {
                log.error("【SMZDM】【" + account.getUsername() + "】签到异常", e);
            }

        }
    }

    private boolean login(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsername();
        String timestamp = System.currentTimeMillis() + "";

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        // nvps.add(new BasicNameValuePair("callback", "jQuery_" + timestamp));
        nvps.add(new BasicNameValuePair("user_login", usrename));
        nvps.add(new BasicNameValuePair("user_pass", account.getPassword()));
        nvps.add(new BasicNameValuePair("rememberme", "0"));
        nvps.add(new BasicNameValuePair("is_third", ""));
        nvps.add(new BasicNameValuePair("is_pop", "1"));
        nvps.add(new BasicNameValuePair("captcha", ""));
        nvps.add(new BasicNameValuePair("_", timestamp));

        URI uri = new URIBuilder(LOGIN_URL).addParameters(nvps).build();

        String loginJson = executor.execute(appendTimeOuts(Request.Get(uri))).returnContent().asString();
        JSONObject loginJsonParseObject = JSON.parseObject(StringUtils.substringBetween(loginJson, "(", ")"));
        if (0 != loginJsonParseObject.getInteger("error_code")) {
            log.info("【SMZDM】【{}】登录失败：{}", usrename, loginJsonParseObject.getJSONObject("error_msg").getString("user_pass"));
            return false;
        }

        log.info("【SMZDM】【{}】登录成功", usrename);
        return true;
    }

    private boolean checkIn(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsername();

        URI checkInURI = new URIBuilder(CHECK_IN_URL).
                addParameter("_", System.currentTimeMillis() + "").
                build();

        String signInJson = executor.execute(appendTimeOuts(Request.Get(checkInURI))).returnContent().asString();
        JSONObject signInParseObject = JSON.parseObject(StringUtils.substringBetween(signInJson, "(", ")"));
        if (0 != signInParseObject.getInteger("error_code")) {
            log.info("【SMZDM】【{}】签到失败：{}", usrename, signInParseObject.getJSONObject("error_msg").getString("public"));
            return false;
        }

        log.info("【SMZDM】【{}】签到成功", usrename);
        return true;
    }
}
