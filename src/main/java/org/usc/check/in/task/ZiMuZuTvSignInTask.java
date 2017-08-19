package org.usc.check.in.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.usc.check.in.model.Account;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Shunli
 */
@Component
@ConfigurationProperties(prefix = "zimuzu")
public class ZiMuZuTvSignInTask extends BaseTask {
    private static final Logger log = LoggerFactory.getLogger(ZiMuZuTvSignInTask.class);

    private static final String URL = "http://www.zmz2017.com/";
    private static final String LOGIN_URL = "http://www.zmz2017.com/User/Login/ajaxLogin";
    private static final String SIGN_IN_URL = "http://www.zmz2017.com/user/login";

    // @Scheduled(cron = "0 0 7,18 * * ?")
    public void run() {
        for (Account account : getAccounts()) {
            try {
                Executor executor = Executor.newInstance().use(new BasicCookieStore());
                login(executor, account);
                if (login(executor, account)) {
                    signIn(executor, account);
                }
            } catch (Exception e) {
                log.error("【ZIMUZU】【" + account.getUsername() + "】签到异常", e);
            }
        }
    }

    private boolean login(Executor executor, Account account) throws ClientProtocolException, IOException {
        // 1st load first page, 313932302c31303830 = stringToHex("1920,1080")
        executor.execute(appendTimeOuts(Request.Get(URL))).discardContent();
        executor.execute(appendTimeOuts(Request.Get(URL + "?security_verify_data=313932302c31303830"))).discardContent();

        String usrename = account.getUsername();

        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("account", usrename));
        formParams.add(new BasicNameValuePair("password", account.getPassword()));
        formParams.add(new BasicNameValuePair("remember", "0"));
        formParams.add(new BasicNameValuePair("url_back", URL));

        String loginJson = executor.execute(appendTimeOuts(Request.Post(LOGIN_URL)).bodyForm(formParams)).returnContent().asString();
        JSONObject loginJsonParseObject = JSON.parseObject(loginJson);
        if (1 != loginJsonParseObject.getInteger("status")) {
            log.info("【ZIMUZU】【{}】登录失败：{}", usrename, loginJsonParseObject.getString("info"));
            return false;
        }

        log.info("【ZIMUZU】【{}】登录成功", usrename);
        return true;
    }

    private boolean signIn(Executor executor, Account account) throws ClientProtocolException, IOException, InterruptedException {
        // String usrename = account.getUsername();

        executor.execute(appendTimeOuts(Request.Get(URL))).discardContent();
        executor.execute(appendTimeOuts(Request.Get("http://www.zimuzu.tv/public/hotkeyword"))).discardContent();
        executor.execute(appendTimeOuts(Request.Get("http://www.zimuzu.tv/user/login/getCurUserTopInfo"))).discardContent();

        // String rtn = executor.execute(appendTimeOuts(Request.Get(SIGN_IN_URL))).returnContent().asString();
        // if (StringUtils.contains(rtn, "您今天已登录")) {
        //     log.info("【ZIMUZU】【{}】签到成功", usrename);
        //     return true;
        // }
        //
        // log.info("【ZIMUZU】【{}】签到失败", usrename);
        return false;
    }

}
