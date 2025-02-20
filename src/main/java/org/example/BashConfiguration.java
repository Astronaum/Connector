package org.example;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

public class BashConfiguration extends AbstractConfiguration {

    private String scriptContent;
    private String scriptPath;
    private String shell;

    @ConfigurationProperty(order = 1, displayMessageKey = "Script Content")
    public String getScriptContent() {
        return scriptContent;
    }

    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    @ConfigurationProperty(order = 2, displayMessageKey = "Script Path")
    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "Shell")
    public String getShell() {
        return shell;
    }

    public void setShell(String shell) {
        this.shell = shell;
    }

    @Override
    public void validate() {
        if ((scriptContent == null || scriptContent.isEmpty()) &&
                (scriptPath == null || scriptPath.isEmpty())) {
            throw new IllegalStateException("Either scriptContent or scriptPath must be provided.");
        }

        if (shell == null || shell.isEmpty()) {
            throw new IllegalStateException("Shell must be specified.");
        }
    }

}
