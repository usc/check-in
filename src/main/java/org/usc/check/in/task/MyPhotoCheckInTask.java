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
public class MyPhotoCheckInTask extends BaseTask {
    private static final Logger log = LoggerFactory.getLogger(MyPhotoCheckInTask.class);

    private static final String LOGIN_URL = "http://ss.myphoto.wang/user/_login.php";
    private static final String CHECK_IN_URL = "http://ss.myphoto.wang/user/_checkin.php";

    @Override
    protected String name() {
        return "myphoto";
    }

    @Scheduled(cron = "0 0 0/2 * * ?")
    public void run() {
        for (Account account : buildAccounts()) {
            try {
                Executor executor = Executor.newInstance().cookieStore(new BasicCookieStore());
                if (login(executor, account)) {
                    checkIn(executor, account);
                }
            } catch (Exception e) {
                log.error("【MyPhoto】【" + account.getUsername() + "】签到异常", e);
            }

        }
    }

    private boolean login(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsername();

        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("email", usrename));
        formParams.add(new BasicNameValuePair("passwd", account.getPassword()));
        formParams.add(new BasicNameValuePair("remember_me", "week"));

        String loginJson = executor.execute(appendTimeOuts(Request.Post(LOGIN_URL)).bodyForm(formParams)).returnContent().asString();
        JSONObject loginJsonParseObject = JSON.parseObject(loginJson);
        if (!StringUtils.equals("1", loginJsonParseObject.getString("code"))) {
            log.info("【MyPhoto】【{}】登录失败：{}", usrename, loginJsonParseObject.getString("msg"));
            return false;
        }

        log.info("【MyPhoto】【{}】登录成功", usrename);
        return true;
    }

    private boolean checkIn(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsername();

        String signInJson = executor.execute(appendTimeOuts(Request.Get(CHECK_IN_URL))).returnContent().asString();
        log.info("【MyPhoto】【{}】签到结果：{}", usrename, JSON.parseObject(signInJson).getString("msg"));
        return true;
    }
}
