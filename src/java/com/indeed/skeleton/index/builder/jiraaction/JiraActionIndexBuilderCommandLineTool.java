package com.indeed.skeleton.index.builder.jiraaction;

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

/**
 * @author soono
 * @author kbinswanger
 */
public class JiraActionIndexBuilderCommandLineTool implements CommandLineTool {
    private static final Logger log = Logger.getLogger(JiraActionIndexBuilderCommandLineTool.class);

    private JiraActionIndexBuilder indexBuilder;

    public static void main(final String[] args) {
        final CommandLineUtil cmdLineUtil = new CommandLineUtil(log, args, new JiraActionIndexBuilderCommandLineTool());
        cmdLineUtil.addStatusUpdateFunction(new CronToolStatusUpdater(
                JiraActionIndexBuilderCommandLineTool.class.getName(),
                JiraActionIndexBuilderCommandLineTool.class.getSimpleName(),
                cmdLineUtil.getArgs(), true));
        cmdLineUtil.run();
    }

    @Override
    public void initialize(final CommandLineUtil cmdLineUtil) {
        final Configuration config = cmdLineUtil.getProperties();
        final String jiraUsername = config.getString("jira.username.indexer");
        final String jiraPassword = config.getString("jira.password.indexer");

        final String jiraBaseUrl = config.getString("jira.baseurl");
        final String[] jiraFieldArray = config.getStringArray("jira.fields");
        final String jiraFields = arrayToCommaDelimetedString(jiraFieldArray);
        final String jiraExpand = config.getString("jira.expand");
        final String[] jiraProjectArray = config.getStringArray("jira.project");
        final String jiraProject = arrayToCommaDelimetedString(jiraProjectArray);
        final String iuploadUrl = config.getString("iupload.url");


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

        final JiraActionIndexBuilderConfig indexBuilderConfig = new JiraActionIndexBuilderConfig(jiraUsername,
                jiraPassword, jiraBaseUrl, jiraFields, jiraExpand, jiraProject, iuploadUrl, startDate, endDate,
                jiraBatchSize);
        indexBuilder = new JiraActionIndexBuilder(indexBuilderConfig);
    }

    private String arrayToCommaDelimetedString(final String[] array) {
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