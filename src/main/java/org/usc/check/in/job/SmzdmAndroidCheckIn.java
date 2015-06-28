package org.usc.check.in.job;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 *
 * @author Shunli
 */
public class SmzdmAndroidCheckIn {
    private static final String LOGIN_URL = "https://api.smzdm.com/v1/user/login";
    private static final String CHECK_IN_URL = "https://api.smzdm.com/v1/user/checkin";

    private static final String SSID = "D8abqe1FRAD2q037uVrvioeMW1Wbc4FV";
    private static final String USER_AGENT = "smzdm_android_V5.6.1 rv:240 (MI 4LTE;Android4.4.4;zh)";

    public static void main(String[] args) {
        List<Account> accounts = ImmutableList.of(
                new Account("account", "password")
                );

        for (Account account : accounts) {
            try {
                Executor executor = Executor.newInstance().cookieStore(new BasicCookieStore());
                String token = login(executor, account);
                if (StringUtils.isNotEmpty(token)) {
                    checkIn(executor, account, token);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("【" + account.getUsrename() + "】签到异常");
            }

        }
    }

    private static String login(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsrename();

        List<NameValuePair> formParams = buildFormParams("");
        formParams.add(new BasicNameValuePair("user_login", usrename));
        formParams.add(new BasicNameValuePair("user_pass", account.getPassword()));

        String loginJson = executor.execute(Request.Post(LOGIN_URL).bodyForm(formParams).userAgent(USER_AGENT)).returnContent().asString();

        JSONObject loginJsonParseObject = JSON.parseObject(loginJson);
        if (0 != loginJsonParseObject.getInteger("error_code")) {
            System.out.println("【" + usrename + "】登录失败：" + loginJsonParseObject.getString("error_msg"));
            return StringUtils.EMPTY;
        }

        System.out.println("【" + usrename + "】登录成功");
        return loginJsonParseObject.getJSONObject("data").getString("token");
    }

    private static boolean checkIn(Executor executor, Account account, String token) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsrename();

        String checkInJson = executor.execute(Request.Post(CHECK_IN_URL).bodyForm(buildFormParams(token)).userAgent(USER_AGENT)).returnContent().asString();

        JSONObject checkInParseObject = JSON.parseObject(checkInJson);
        if (0 != checkInParseObject.getInteger("error_code")) {
            System.out.println("【" + usrename + "】签到失败：" + checkInParseObject.getString("error_msg"));
            return false;
        }

        System.out.println("【" + usrename + "】签到成功");
        return true;
    }

    private static List<NameValuePair> buildFormParams(String token) {
        List<NameValuePair> formParams = Lists.newArrayList();
        formParams.add(new BasicNameValuePair("captcha", ""));
        formParams.add(new BasicNameValuePair("f", "android"));
        formParams.add(new BasicNameValuePair("s", SSID));
        formParams.add(new BasicNameValuePair("token", token));
        formParams.add(new BasicNameValuePair("partner_id", "3"));
        formParams.add(new BasicNameValuePair("v", "240"));
        formParams.add(new BasicNameValuePair("", ""));
        return formParams;
    }

    static class Account {
        private String usrename;
        private String password;

        public Account(String usrename, String password) {
            this.usrename = usrename;
            this.password = password;
        }

        public String getUsrename() {
            return usrename;
        }

        public String getPassword() {
            return password;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }
}
