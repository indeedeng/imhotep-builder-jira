package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.common.cli.CommandLineTool;
import com.indeed.common.cli.CommandLineUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

/**
 * Created by soono on 9/15/16.
 */
public class JiraActionsTool implements CommandLineTool {
    private static final Logger log = Logger.getLogger(JiraActionsTool.class);
    private String jiraUsername;

    // For debugging
    public static void main(String[] args) {
        final JiraActionsTool u = new JiraActionsTool();
        CommandLineUtil cmdLineUtil = new CommandLineUtil(log, args, u);
        u.initialize(cmdLineUtil);
    }

    @Override
    public void initialize(CommandLineUtil cmdLineUtil) {
        final Configuration props = cmdLineUtil.getProperties();
        jiraUsername = props.getString("jiraUsername");
    }

    @Override
    public void run(CommandLineUtil cmdLineUtil) {
        // FIXME: Not sure what to put here.
    }

    public String getJiraUsername() {
        return jiraUsername;
    }
}