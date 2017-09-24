package org.usc.check.in.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.usc.check.in.model.Account;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Shunli
 */
@Component
@ConfigurationProperties(prefix = "smzdm")
public class SmzdmCheckInTask extends BaseTask {
    private static final Logger log = LoggerFactory.getLogger(SmzdmCheckInTask.class);

    private static final String LOGIN_URL = "https://zhiyou.smzdm.com/user/login/ajax_check";
    private static final String CHECK_IN_URL = "http://zhiyou.smzdm.com/user/checkin/jsonp_checkin";

    // @Scheduled(cron = "0 0 5,18 * * ?")
    public void run() {
        for (Account account : getAccounts()) {
            try {
                CloseableHttpClient client = HttpClients.custom().setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
                Executor executor = Executor.newInstance(client).use(new BasicCookieStore());
                if(login(executor, account)) {
                    checkIn(executor, account);
                }
            } catch (Exception e) {
                log.error("【SMZDM】【" + account.getUsername() + "】签到异常", e);
            }

        }
    }

    private boolean login(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsername();

        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("username", usrename));
        formParams.add(new BasicNameValuePair("password", account.getPassword()));
        formParams.add(new BasicNameValuePair("rememberme", "on"));
        formParams.add(new BasicNameValuePair("captcha", ""));
        formParams.add(new BasicNameValuePair("redirect_url", "http://www.smzdm.com"));

        String loginJson = executor.execute(appendTimeOuts(Request.Post(LOGIN_URL)).bodyForm(formParams)).returnContent().asString();
        JSONObject loginJsonParseObject = JSON.parseObject(loginJson);
        if(0 != loginJsonParseObject.getInteger("error_code")) {
            log.info("【SMZDM】【{}】登录失败：{}", usrename, loginJsonParseObject.getString("error_msg"));
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
        JSONObject signInParseObject = JSON.parseObject(signInJson);
        if(0 != signInParseObject.getInteger("error_code")) {
            log.info("【SMZDM】【{}】签到失败：{}", usrename, signInParseObject.getString("error_msg"));
            return false;
        }

        log.info("【SMZDM】【{}】签到成功", usrename);
        return true;
    }
}
