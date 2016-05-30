package org.usc.check.in;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public XMLConfiguration config() {
        try {
            return buildConfig("myConfig.xml");
        } catch (ConfigurationException e1) {
            try {
                return buildConfig("config.xml");
            } catch (ConfigurationException e2) {
                throw new RuntimeException("can't find myConfig.xml(default) or config.xml in classpath");
            }
        }
    }

    private XMLConfiguration buildConfig(String fileName) throws ConfigurationException {
        XMLConfiguration config = new XMLConfiguration(fileName);
        config.setReloadingStrategy(new FileChangedReloadingStrategy());

        return config;
    }
}
