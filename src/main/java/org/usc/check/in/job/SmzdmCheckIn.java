package org.usc.check.in.job;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
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
public class SmzdmCheckIn {
    private static final String LOGIN_URL = "http://www.smzdm.com/user/login/jsonp_check";
    private static final String CHECK_IN_URL = "http://www.smzdm.com/user/qiandao/jsonp_checkin";

    public static void main(String[] args) {
        List<Account> accounts = ImmutableList.of(
                new Account("account", "password")
                );

        for (Account account : accounts) {
            try {
                Executor executor = Executor.newInstance().cookieStore(new BasicCookieStore());
                if (login(executor, account)) {
                    checkIn(executor, account);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("【" + account.getUsrename() + "】签到异常");
            }

        }
    }
    private static boolean login(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsrename();
        String timestamp = System.currentTimeMillis() + "";

        List<NameValuePair> nvps = Lists.newArrayList();
        // nvps.add(new BasicNameValuePair("callback", "jQuery_" + timestamp));
        nvps.add(new BasicNameValuePair("user_login", usrename));
        nvps.add(new BasicNameValuePair("user_pass", account.getPassword()));
        nvps.add(new BasicNameValuePair("rememberme", "0"));
        nvps.add(new BasicNameValuePair("is_third", ""));
        nvps.add(new BasicNameValuePair("is_pop", "1"));
        nvps.add(new BasicNameValuePair("captcha", ""));
        nvps.add(new BasicNameValuePair("_", timestamp));

        URI uri = new URIBuilder(LOGIN_URL).addParameters(nvps).build();

        String loginJson = executor.execute(Request.Get(uri)).returnContent().asString();
        JSONObject loginJsonParseObject = JSON.parseObject(StringUtils.substringBetween(loginJson, "(", ")"));
        if (0 != loginJsonParseObject.getInteger("error_code")) {
            System.out.println("【" + usrename + "】登录失败：" + loginJsonParseObject.getJSONObject("error_msg").getString("user_pass"));
            return false;
        }

        System.out.println("【" + usrename + "】登录成功");
        return true;
    }

    private static boolean checkIn(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsrename();

        URI checkInURI = new URIBuilder(CHECK_IN_URL).
                addParameter("_", System.currentTimeMillis() + "").
                build();

        String signInJson = executor.execute(Request.Get(checkInURI)).returnContent().asString();
        JSONObject signInParseObject = JSON.parseObject(StringUtils.substringBetween(signInJson, "(", ")"));
        if (0 != signInParseObject.getInteger("error_code")) {
            System.out.println("【" + usrename + "】签到失败：" + signInParseObject.getJSONObject("error_msg").getString("public"));
            return false;
        }

        System.out.println("【" + usrename + "】签到成功");
        return true;
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
