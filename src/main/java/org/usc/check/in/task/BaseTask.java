package org.usc.check.in.task;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.http.client.fluent.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.usc.check.in.model.Account;
import org.usc.check.in.util.DesUtil;

/**
 *
 * @author Shunli
 */
public abstract class BaseTask {
    @Autowired
    private XMLConfiguration config;

    protected abstract String name();

    protected List<Account> buildAccounts() {
        String keyPrefix = name();

        List<Account> accounts = new ArrayList<Account>();
        if (config.getBoolean(keyPrefix + ".switch", true)) {
            List<HierarchicalConfiguration> configurationsAt = config.configurationsAt(keyPrefix + ".accounts.account");
            for (HierarchicalConfiguration hierarchicalConfiguration : configurationsAt) {
                try {
                    String username = hierarchicalConfiguration.getString("username");
                    String encryptPassword = hierarchicalConfiguration.getString("password");
                    String password = DesUtil.decrypt(encryptPassword, keyPrefix + username);

                    accounts.add(new Account(username, password));
                } catch (Exception e) {
                }
            }
        }

        return accounts;
    }
    protected Request appendTimeOuts(Request request) {
        return request.connectTimeout(config.getInt("timeouts.connect", 60000)).socketTimeout(config.getInt("timeouts.socket", 60000));
    }
}
