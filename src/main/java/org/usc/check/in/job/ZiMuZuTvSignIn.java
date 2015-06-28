package org.usc.check.in.job;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
public class ZiMuZuTvSignIn {
    private static final String URL = "http://www.zimuzu.tv/";
    private static final String LOGIN_URL = "http://www.zimuzu.tv/User/Login/ajaxLogin";
    private static final String SIGN_IN_PAGE_URL = "http://www.zimuzu.tv/user/sign";
    private static final String SIGN_IN_URL = "http://www.zimuzu.tv/user/sign/dosign";

    public static void main(String[] args) {
        List<Account> accounts = ImmutableList.of(
                new Account("account", "password")
                );

        for (Account account : accounts) {
            try {
                Executor executor = Executor.newInstance().cookieStore(new BasicCookieStore());
                if (login(executor, account)) {
                    signIn(executor, account);
                }
            } catch (Exception e) {
                System.out.println("【" + account.getUsrename() + "】签到异常");
            }

        }
    }

    private static boolean login(Executor executor, Account account) throws ClientProtocolException, IOException {
        String usrename = account.getUsrename();

        List<NameValuePair> formParams = Lists.newArrayList();
        formParams.add(new BasicNameValuePair("account", usrename));
        formParams.add(new BasicNameValuePair("password", account.getPassword()));
        formParams.add(new BasicNameValuePair("remember", "1"));
        formParams.add(new BasicNameValuePair("url_back", URL));

        String loginJson = executor.execute(Request.Post(LOGIN_URL).bodyForm(formParams)).returnContent().asString();
        JSONObject loginJsonParseObject = JSON.parseObject(loginJson);
        if (1 != loginJsonParseObject.getInteger("status")) {
            System.out.println("【" + usrename + "】登录失败：" + loginJsonParseObject.getString("info"));
            return false;
        }

        System.out.println("【" + usrename + "】登录成功");
        return true;
    }

    private static boolean signIn(Executor executor, Account account) throws ClientProtocolException, IOException, InterruptedException {
        String usrename = account.getUsrename();

        // first load signin page
        executor.execute(Request.Get(SIGN_IN_PAGE_URL));

        // sleep 15s+
        TimeUnit.SECONDS.sleep(20);

        String signInJson = executor.execute(Request.Get(SIGN_IN_URL)).returnContent().asString();
        JSONObject signInParseObject = JSON.parseObject(signInJson);
        Integer stauts = signInParseObject.getInteger("status");
        if (stauts != null) {
            if (0 == stauts) {
                System.out.println("【" + usrename + "】已经签到");
                return true;
            }

            if (1 == stauts) {
                System.out.println("【" + usrename + "】签到成功");
                return true;
            }
        }

        System.out.println("【" + usrename + "】签到失败：" + signInParseObject.getString("info"));
        return false;
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
