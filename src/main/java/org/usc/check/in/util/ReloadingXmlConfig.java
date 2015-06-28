package org.usc.check.in.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

/**
 * XML配置文件的自动加载
 *
 * @author Shunli
 */
public class ReloadingXmlConfig {
    /**
     * 获取自动加载的XML配置文件
     *
     * @param fileName
     *            文件名
     * @return
     */
    public static XMLConfiguration getConfig(String fileName) {
        XMLConfiguration config = null;
        try {
            config = new XMLConfiguration(fileName);
        } catch (ConfigurationException e) {
            throw new RuntimeException("Auto-Reloading-Xml-Config-Failed-Load: " + fileName, e);
        }
        config.setReloadingStrategy(new FileChangedReloadingStrategy());

        return config;
    }
}
