package org.usc.check.in;

import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.usc.check.in.util.ReloadingXmlConfig;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

/**
 *
 * @author Shunli
 */
public class Test {

    public static void main(String[] args) {
        // String salt = StringUtils.remove(UUID.randomUUID().toString(), "-");
        //
        // String encode = BaseEncoding.base64().encode(("test" + salt).getBytes(Charsets.UTF_8));
        // System.out.println(salt);
        // System.out.println(encode);
        // System.out.println(StringUtils.substringBeforeLast(new String(BaseEncoding.base64().decode(encode), Charsets.UTF_8), salt));

        XMLConfiguration config = ReloadingXmlConfig.getConfig("config.xml");

        List<HierarchicalConfiguration> configurationsAt = config.configurationsAt("v2ex.accounts.account");
        for (HierarchicalConfiguration hierarchicalConfiguration : configurationsAt) {
            String username = hierarchicalConfiguration.getString("username");
            String salt = hierarchicalConfiguration.getString("salt");
            String encode = hierarchicalConfiguration.getString("password");
            String password = StringUtils.substringBeforeLast(new String(BaseEncoding.base64().decode(encode), Charsets.UTF_8), salt);

            System.out.println(username);
            System.out.println(encode);
            System.out.println(salt);
            System.out.println(password);
        }

    }
}
