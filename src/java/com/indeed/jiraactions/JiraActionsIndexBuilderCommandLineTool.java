package com.indeed.jiraactions;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.common.base.IndeedSystemProperty;
import com.indeed.common.cli.CommandLineTool;
import com.indeed.common.cli.CommandLineUtil;
import com.indeed.common.dbutil.CronToolStatusUpdater;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;

/**
 * @author soono
 * @author kbinswanger
 */
public class JiraActionsIndexBuilderCommandLineTool implements CommandLineTool {
    private static final Logger log = Logger.getLogger(JiraActionsIndexBuilderCommandLineTool.class);

    private JiraActionsIndexBuilder indexBuilder;

    public static void main(final String[] args) {
        final CommandLineUtil cmdLineUtil = new CommandLineUtil(log, args, new JiraActionsIndexBuilderCommandLineTool());
        cmdLineUtil.addStatusUpdateFunction(new CronToolStatusUpdater(
                JiraActionsIndexBuilderCommandLineTool.class.getName(),
                JiraActionsIndexBuilderCommandLineTool.class.getSimpleName(),
                cmdLineUtil.getArgs(), true));
        cmdLineUtil.run();
    }

    @Override
    public void initialize(final CommandLineUtil cmdLineUtil) {
        final Configuration config = cmdLineUtil.getProperties();
        final String jiraUsername;
        final String jiraPassword;
        if(IndeedSystemProperty.INSTANCE.toString().contains("Apache")) {
            jiraUsername = config.getString("apachejira.username.indexer");
            jiraPassword = config.getString("apachejira.password.indexer");
        } else {
            jiraUsername = config.getString("jira.username.indexer");
            jiraPassword = config.getString("jira.password.indexer");
        }

        final String jiraBaseUrl = config.getString("jira.baseurl");
        final String[] jiraFieldArray = config.getStringArray("jira.fields");
        final String jiraFields = arrayToCommaDelimetedString(jiraFieldArray);
        final String jiraExpand = config.getString("jira.expand");
        final String[] jiraProjectArray = config.getStringArray("jira.project");
        final String jiraProject = arrayToCommaDelimetedString(jiraProjectArray);
        final String[] excludedJiraProjectArray = config.getStringArray("jira.projectexcluded");
        final String excludedJiraProject = arrayToCommaDelimetedString(excludedJiraProjectArray);
        final String iuploadUrl = config.getString("iupload.url");
        final String iuploadUsername = config.getString("jira.username.indexer");
        final String iuploadPassword = config.getString("jira.password.indexer");
        final String indexName = config.getString("indexname");
        final boolean ignoreCustomFields = config.getBoolean("ignorecustomfields");

        @SuppressWarnings("AccessStaticViaInstance")
        final Options options = new Options().addOption((OptionBuilder
                    .withLongOpt("start")
                    .isRequired()
                    .hasArg()
                    .withArgName("YYYY-MM-DD")
                    .withDescription("ISO-8601 Formatted date string specifying the start date (inclusive"))
                    .create("start"))
        .addOption(OptionBuilder
                    .withLongOpt("end")
                    .isRequired()
                    .hasArg()
                    .withArgName("YYYY-MM-DD")
                    .withDescription("ISO-8601 Formatted date string specifying end date (exclusive")
                    .create("end"))
         .addOption(OptionBuilder
                    .withLongOpt("jiraBatchSize")
                    .isRequired()
                    .hasArg()
                    .withDescription("Number of issues to retrieve in each batch")
                    .create("jiraBatchSize"));

        final String startDate;
        final String endDate;
        final int jiraBatchSize;
        final CommandLineParser parser = new GnuParser();
        final CommandLine commandLineArgs;
        try {
            commandLineArgs = parser.parse(options, cmdLineUtil.getArgs());
            startDate = commandLineArgs.getOptionValue("start");
            endDate = commandLineArgs.getOptionValue("end");
            jiraBatchSize = Integer.parseInt(commandLineArgs.getOptionValue("jiraBatchSize"));
        } catch (final ParseException e) {
            log.error("Threw an exception trying to run the index builder", e);
            System.exit(-1);
            return; // For some reason this makes some errors go away, even though it never gets hit
        }

        final JiraActionsIndexBuilderConfig indexBuilderConfig = new JiraActionsIndexBuilderConfig(jiraUsername,
                jiraPassword, jiraBaseUrl, jiraFields, jiraExpand, jiraProject, excludedJiraProject, iuploadUrl,
                iuploadUsername, iuploadPassword, startDate, endDate, jiraBatchSize, indexName, ignoreCustomFields);
        indexBuilder = new JiraActionsIndexBuilder(indexBuilderConfig);
    }

    @VisibleForTesting
    protected static String arrayToCommaDelimetedString(@Nullable final String[] array) {
        if(array == null) {
            return "";
        }

        final StringBuilder builder = new StringBuilder();
        for (final String token : array) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(token);
        }
            return builder.toString();
    }

    @Override
    public void run(final CommandLineUtil cmdLineUtil) {
        try {
            indexBuilder.run();
        } catch (final Exception e) {
            System.exit(-1);
        }
    }
}