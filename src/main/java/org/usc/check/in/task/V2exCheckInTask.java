package org.usc.check.in.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.undertow.util.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.usc.check.in.model.Account;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author Shunli
 */
@Component
@ConfigurationProperties(prefix = "v2ex")
public class V2exCheckInTask extends BaseTask {
    private static final Logger log = LoggerFactory.getLogger(V2exCheckInTask.class);

    private static final String LOGIN_URL = "https://www.v2ex.com/signin";
    private static final String CHECK_IN_URL = "https://www.v2ex.com/mission/daily";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36";

    private String cookiePath = "/tmp";

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    @Scheduled(cron = "0 0 9,18 * * ?")
    public void run() {
        for (Account account : getAccounts()) {
            try {
                RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD_STRICT).build();
                CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build();
                Executor executor = Executor.newInstance(client);
                if(login(executor, account)) {
                    checkIn(executor, account);
                }
            } catch (Exception e) {
                log.error("【V2EX】【" + account.getUsername() + "】签到异常", e);
            }

        }
    }

    private BasicCookieStore getCookieStore(String cookieJson) {
        JSONArray jsonArray = JSON.parseArray(cookieJson);

        BasicCookieStore cookieStore = new BasicCookieStore();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String name = jsonObject.getString("name");
            String value = jsonObject.getString("value");

            BasicClientCookie cookie = new BasicClientCookie(name, value);
            cookie.setDomain(".v2ex.com");
            cookie.setPath("/");

            cookieStore.addCookie(cookie);
        }

        return cookieStore;
    }

    private boolean login(Executor executor, Account account) throws IOException {
        String userName = account.getUsername();

        File file = new File(cookiePath, "v2ex-" + userName + "-cookie.json");
        if(!file.exists()) {
            log.info("【V2EX】【{}】没有cookie文件", userName);
            return false;
        }

        executor.use(getCookieStore(FileUtils.readFile(new FileInputStream(file))));

        log.info("【V2EX】【{}】加载cookie文件成功", userName);
        return true;
    }

    // private boolean login(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
    //     String usrename = account.getUsername();
    //
    //     // 1st get once
    //     Document checkLoginOnce = Jsoup.parse(executor.execute(appendTimeOuts(Request.Get(LOGIN_URL))).returnContent().asString());
    //     String once = checkLoginOnce.getElementsByAttributeValue("name", "once").attr("value");
    //     Elements elementsByClass = checkLoginOnce.getElementsByClass("sl");
    //
    //     List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    //     formParams.add(new BasicNameValuePair(elementsByClass.get(0).attr("name"), usrename));
    //     formParams.add(new BasicNameValuePair(elementsByClass.get(1).attr("name"), account.getPassword()));
    //     formParams.add(new BasicNameValuePair("once", once));
    //     formParams.add(new BasicNameValuePair("next", "/"));
    //
    //     // login
    //     executor.execute(appendTimeOuts(Request.Post(LOGIN_URL)).bodyForm(formParams).userAgent(USER_AGENT).addHeader("Referer", "http://www/v2ex.com/signin")).discardContent();
    //
    //     // checkIn must load first page once
    //     String rtn = executor.execute(appendTimeOuts(Request.Get("http://www.v2ex.com"))).returnContent().asString();
    //     if(StringUtils.contains(rtn, "signout")) {
    //         log.info("【V2EX】【{}】登录成功", usrename);
    //         return true;
    //     }
    //
    //     log.info("【V2EX】【{}】登录失败", usrename);
    //     return false;
    // }

    private boolean checkIn(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException, InterruptedException {
        String usrename = account.getUsername();

        String rtn = executor.execute(appendTimeOuts(Request.Get(CHECK_IN_URL))).returnContent().asString();
        if(StringUtils.contains(rtn, "fa-ok-sign")) {
            log.info("【V2EX】【{}】每日登录奖励已领取，当前账户余额：{}", usrename, getBalance(rtn));
            return true;
        }

        Elements element = Jsoup.parse(rtn).getElementsByAttributeValueMatching("onclick", "/mission/daily/redeem");
        String once = StringUtils.substringBetween(element.attr("onclick"), "'", "'");
        if(StringUtils.isNotEmpty(once)) {
            String url = "http://www.v2ex.com" + once;

            String checkInRtn = executor.execute(appendTimeOuts(Request.Get(url)).userAgent(USER_AGENT).addHeader("Referer", CHECK_IN_URL)).returnContent().asString();
            log.info("【V2EX】【{}】签到成功，当前账户余额：{}", usrename, getBalance(checkInRtn));
            return true;
        }

        log.info("【V2EX】【{}】签到成功", usrename);
        return false;
    }

    private String getBalance(String rtn) {
        return Jsoup.parse(rtn).getElementsByClass("balance_area").text();
    }

}
