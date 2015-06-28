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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 *
 * @author Shunli
 */
public class V2exCheckIn {
    private static final String LOGIN_URL = "http://www.v2ex.com/signin";
    private static final String CHECK_IN_URL = "http://www.v2ex.com/mission/daily";

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36";

    public static void main(String[] args) {
        List<Account> accounts = ImmutableList.of(
                new Account("account", "password")
                );

        for (Account account : accounts) {
            try {
                BasicCookieStore cookieStore = new BasicCookieStore();
                Executor executor = Executor.newInstance().cookieStore(cookieStore);
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

        // 1st get once
        Document checkLoginOnce = Jsoup.parse(executor.execute(Request.Get(LOGIN_URL)).returnContent().asString());
        String once = checkLoginOnce.getElementsByAttributeValue("name", "once").attr("value");

        List<NameValuePair> formParams = Lists.newArrayList();
        formParams.add(new BasicNameValuePair("u", usrename));
        formParams.add(new BasicNameValuePair("p", account.getPassword()));
        formParams.add(new BasicNameValuePair("once", once));
        formParams.add(new BasicNameValuePair("next", "/"));

        // login
        executor.execute(Request.Post(LOGIN_URL).bodyForm(formParams).userAgent(USER_AGENT).addHeader("Referer", "http://www/v2ex.com/signin"));

        // checkIn must load first page once
        String rtn = executor.execute(Request.Get("http://www.v2ex.com")).returnContent().asString();
        if (StringUtils.contains(rtn, "signout")) {
            System.out.println("【" + usrename + "】登录成功");
            return true;
        }

        System.out.println("【" + usrename + "】登录失败");
        return false;
    }

    private static boolean checkIn(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException, InterruptedException {
        String usrename = account.getUsrename();

        String rtn = executor.execute(Request.Get(CHECK_IN_URL)).returnContent().asString();
        if (StringUtils.contains(rtn, "fa-ok-sign")) {
            System.out.println("【" + usrename + "】每日登录奖励已领取，当前账户余额：" + getBalance(rtn));
            return true;
        }

        Elements element = Jsoup.parse(rtn).getElementsByAttributeValueMatching("onclick", "/mission/daily/redeem");
        String once = StringUtils.substringBetween(element.attr("onclick"), "'", "'");
        if (StringUtils.isNotEmpty(once)) {
            String url = "http://www.v2ex.com" + once;

            String checkInRtn = executor.execute(Request.Get(url).userAgent(USER_AGENT).addHeader("Referer", CHECK_IN_URL)).returnContent().asString();
            System.out.println("【" + usrename + "】签到成功，当前账户余额：" + getBalance(checkInRtn));

            return true;
        }

        System.out.println("【" + usrename + "】签到失败");
        return false;
    }

    private static String getBalance(String rtn) {
        return Jsoup.parse(rtn).getElementsByClass("balance_area").text();
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
