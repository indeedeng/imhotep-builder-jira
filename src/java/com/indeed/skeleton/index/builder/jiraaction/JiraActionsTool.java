package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.common.cli.CommandLineTool;
import com.indeed.common.cli.CommandLineUtil;
import com.indeed.common.dbutil.CronToolStatusUpdater;
import com.indeed.common.util.ConfigurationParser;
import org.apache.log4j.Logger;

/**
 * Created by soono on 9/15/16.
 */
public class JiraActionsTool implements CommandLineTool {
    private static final Logger log = Logger.getLogger(JiraActionsTool.class);

    public static void main(String[] args) {
        System.out.println("start jira actions tool main");
        final JiraActionsTool u = new JiraActionsTool();
        CommandLineUtil cmdLineUtil = new CommandLineUtil(log, args, u);
        final String toolFullName = u.getClass().getName();
        final String toolDisplayName = u.getClass().getSimpleName();
        cmdLineUtil.addStatusUpdateFunction(new CronToolStatusUpdater(cmdLineUtil.getProperties(), toolFullName, toolDisplayName, args, true));
        System.out.println("start jira actions tool run");
        u.run(cmdLineUtil);
        System.out.println("end jira actions tool main and run");
    }

    @Override
    public void initialize(CommandLineUtil cmdLineUtil) {
    }

    @Override
    public void run(CommandLineUtil cmdLineUtil) {
        try {
            final Config config = new Config();
            ConfigurationParser.readFromPath(config, cmdLineUtil.getPropertiesFilename(), cmdLineUtil.getArgs());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}