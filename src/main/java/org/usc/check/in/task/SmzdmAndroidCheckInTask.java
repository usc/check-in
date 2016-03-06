package org.usc.check.in.task;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.usc.check.in.model.Account;
import org.usc.check.in.util.DesUtil;

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
    private static final String USER_INFO_URL = "https://api.smzdm.com/v1/user/info";
    private static final String LOTTERY_CHECK_IN_URL = "https://h5.smzdm.com/user/lottery/checkin";
    private static final String LOTTERY_URL = "https://h5.smzdm.com/user/lottery/ajax_draw";

    private static final String SSID = "D8abqe1FRAD2q037uVrvioeMW1Wbc4FV";
    private static final String USER_AGENT = "smzdm_android_V6.2 rv:310 (MI 4LTE;Android6.0.1;zh)";

    @Override
    protected String name() {
        return "smzdm";
    }

    @Scheduled(cron = "0 0 5,18 * * ?")
    public void run() {
        for (Account account : buildAccounts()) {
            try {
                CloseableHttpClient client = HttpClients.custom().setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).build();
                Executor executor = Executor.newInstance(client).cookieStore(new BasicCookieStore());
                String token = login(executor, account);
                if (StringUtils.isNotEmpty(token)) {
                    if (checkIn(executor, account, token)) {
                        lottery(executor, account, token);
                    }
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

        Request request = appendTimeOuts(Request.Post(LOGIN_URL)).bodyForm(formParams).userAgent(USER_AGENT);
        JSONObject loginJsonParseObject = JSON.parseObject(executor.execute(request).returnContent().asString());

        return parseResult("登录", usrename, loginJsonParseObject) ? loginJsonParseObject.getJSONObject("data").getString("token") : StringUtils.EMPTY;
    }

    private boolean checkIn(Executor executor, Account account, String token) throws ClientProtocolException, IOException, URISyntaxException {
        Request request = appendTimeOuts(Request.Post(CHECK_IN_URL)).bodyForm(buildFormParams(token)).userAgent(USER_AGENT);
        JSONObject checkInParseObject = JSON.parseObject(executor.execute(request).returnContent().asString());

        return parseResult("签到", account.getUsername(), checkInParseObject);
    }

    private boolean lottery(Executor executor, Account account, String token) throws Exception {
        String usrename = account.getUsername();

        // 1st get user info
        Request userInfoRequest = appendTimeOuts(Request.Post(USER_INFO_URL)).bodyForm(buildFormParams(token)).userAgent(USER_AGENT);
        JSONObject userInfoParseObject = JSON.parseObject(executor.execute(userInfoRequest).returnContent().asString());
        if (!parseResult("获取信息", usrename, userInfoParseObject)) {
            return false;
        }

        JSONObject userInfoDataJsonObject = userInfoParseObject.getJSONObject("data");
        String serverTime = userInfoDataJsonObject.getString("server_time");
        String key = userInfoDataJsonObject.getString("en_key");
        String userId = userInfoDataJsonObject.getString("user_smzdm_id");

        URI lottreyCheckInURI = new URIBuilder(LOTTERY_CHECK_IN_URL).
                addParameter("d", DesUtil.encrypt(userId + SSID + "_" + serverTime, key)).
                addParameter("t", DigestUtils.md5Hex(token)).
                addParameter("f", "android").
                addParameter("s", SSID).
                addParameter("add_point", "").
                addParameter("displaymode", "0").
                build();

        // 2nd check in lottery status and set cookie
        executor.execute(appendTimeOuts(Request.Get(lottreyCheckInURI)).userAgent(USER_AGENT)).discardContent();

        // 3rd lottery
        Request lotteryRequest = appendTimeOuts(Request.Post(LOTTERY_URL)).userAgent(USER_AGENT);
        JSONObject lotteryParseObject = JSON.parseObject(executor.execute(lotteryRequest).returnContent().asString());

        return parseResult("抽奖", usrename, lotteryParseObject);
    }

    private static List<NameValuePair> buildFormParams(String token) {
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("captcha", ""));
        formParams.add(new BasicNameValuePair("f", "android"));
        formParams.add(new BasicNameValuePair("s", SSID));
        formParams.add(new BasicNameValuePair("token", token));
        // formParams.add(new BasicNameValuePair("partner_id", "3"));
        formParams.add(new BasicNameValuePair("weixin", "1"));
        formParams.add(new BasicNameValuePair("v", "310"));
        formParams.add(new BasicNameValuePair("", ""));
        return formParams;
    }

    private boolean parseResult(String action, String usrename, JSONObject jsonObject) {
        if (0 != jsonObject.getInteger("error_code")) {
            log.info("【SMZDM】【{}】{}失败：{}", usrename, action, jsonObject.getString("error_msg"));
            return false;
        }

        log.info("【SMZDM】【{}】{}成功：{}", usrename, action, jsonObject.getString("error_msg"));
        return true;
    }

}
