package org.usc.check.in.task;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.usc.check.in.model.Account;

/**
 *
 * @author Shunli
 */
@Component
public class MiaoCheckInTask extends BaseTask {
    private static final Logger log = LoggerFactory.getLogger(MiaoCheckInTask.class);

    private static final String LOGIN_URL = "http://www.miaoss.net/login.php";
    private static final String CHECK_IN_URL = "http://www.miaoss.net/api.php?cmd=gift500mb";

    @Override
    protected String name() {
        return "miao";
    }

    private static final ResponseHandler<String> UTF8_CONTENT_HANDLER = new ResponseHandler<String>() {
        @Override
        public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            final StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 300) {
                throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                return StringUtils.trim(EntityUtils.toString(entity, "UTF-8"));
            }

            return StringUtils.EMPTY;
        }
    };

    @Scheduled(cron = "0 0 10,18 * * ?")
    public void run() {
        for (Account account : buildAccounts()) {
            try {
                Executor executor = Executor.newInstance().cookieStore(new BasicCookieStore());
                if (login(executor, account)) {
                    checkIn(executor, account);
                }
            } catch (Exception e) {
                log.error("【Miao】【" + account.getUsername() + "】签到异常", e);
            }

        }
    }

    private boolean login(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsername();

        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("email", usrename));
        formParams.add(new BasicNameValuePair("pass", account.getPassword()));
        formParams.add(new BasicNameValuePair("remember", "on"));

        executor.execute(appendTimeOuts(Request.Post(LOGIN_URL)).bodyForm(formParams)).discardContent();

        log.info("【Miao】【{}】登录成功", usrename);
        return true;
    }

    private boolean checkIn(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String signInJson = executor.execute(appendTimeOuts(Request.Get(CHECK_IN_URL))).handleResponse(UTF8_CONTENT_HANDLER);
        log.info("【Miao】【{}】签到结果：{}", account.getUsername(), signInJson);
        return true;
    }
}
