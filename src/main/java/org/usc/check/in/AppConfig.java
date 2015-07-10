package org.usc.check.in;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.usc.check.in.task.SmzdmAndroidCheckInTask;
import org.usc.check.in.task.SmzdmCheckInTask;
import org.usc.check.in.task.V2exCheckInTask;
import org.usc.check.in.task.ZiMuZuTvSignInTask;

@Configuration
@EnableScheduling
public class AppConfig {
    @Bean
    public SmzdmAndroidCheckInTask smzdmAndroidCheckInTask() {
        return new SmzdmAndroidCheckInTask();
    }

    @Bean
    public SmzdmCheckInTask smzdmCheckInTask() {
        return new SmzdmCheckInTask();
    }

    @Bean
    public V2exCheckInTask v2exCheckInTask() {
        return new V2exCheckInTask();
    }

    @Bean
    public ZiMuZuTvSignInTask ziMuZuTvSignInTask() {
        return new ZiMuZuTvSignInTask();
    }

    @Bean
    public XMLConfiguration config() {
        String fileName = "config.xml";

        try {
            XMLConfiguration config = new XMLConfiguration(fileName);
            config.setReloadingStrategy(new FileChangedReloadingStrategy());

            return config;
        } catch (ConfigurationException e) {
            throw new RuntimeException("Xml-Config-Failed-Load: " + fileName, e);
        }
    }
}
