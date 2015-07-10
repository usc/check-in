package org.usc.check.in;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ComponentScan("org.usc")
@EnableScheduling
public class AppConfig {
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
