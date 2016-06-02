package org.usc.check.in.task;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.fluent.Request;
import org.springframework.beans.factory.annotation.Value;
import org.usc.check.in.model.Account;

/**
 *
 * @author Shunli
 */
public abstract class BaseTask {
    @Value("${timeouts.connect:60000}")
    private int connectTimeout;

    @Value("${timeouts.socket:60000}")
    private int socketTimeout;

    private List<Account> accounts = new ArrayList<Account>();

    public List<Account> getAccounts() {
        return accounts;
    }

    protected Request appendTimeOuts(Request request) {
        return request.connectTimeout(connectTimeout).socketTimeout(socketTimeout);
    }

}
