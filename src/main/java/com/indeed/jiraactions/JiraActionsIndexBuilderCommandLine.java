package com.indeed.jiraactions;

import com.google.common.base.Joiner;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinitionParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * @author soono
 * @author kbinswanger
 * @author jack
 */
public class JiraActionsIndexBuilderCommandLine {
    private static final Logger LOGGER = LoggerFactory.getLogger(JiraActionsIndexBuilderCommandLine.class);
    private static final Joiner COMMA_JOINER = Joiner.on(',');

    private JiraActionsIndexBuilder indexBuilder;

    public static void main(final String[] args) {
        final JiraActionsIndexBuilderCommandLine tool = new JiraActionsIndexBuilderCommandLine();
        tool.initialize(args);
        tool.run();
    }

    private void initialize(final String[] args) {
        final Options options = new Options()
                .addOption(Option.builder("p")
                        .longOpt("props")
                        .desc("path to imhotep-jira.properties file")
                        .hasArg()
                        .numberOfArgs(1)
                        .required()
                        .build()
                ).addOption(Option.builder("s")
                        .longOpt("start")
                        .desc("ISO-8601 formatted date string specifying the start date (inclusive)")
                        .hasArg()
                        .numberOfArgs(1)
                        .required()
                        .build()
                ).addOption(Option.builder("e")
                        .longOpt("end")
                        .desc("ISO-8601 formatted date string specifying the end date (exclusive)")
                        .hasArg()
                        .numberOfArgs(1)
                        .required()
                        .build()
                ).addOption(Option.builder("jb")
                        .longOpt("jiraBatchSize")
                        .desc("Number of issues to retrieve in each batch")
                        .hasArg()
                        .numberOfArgs(1)
                        .required()
                        .build());
        final String startDate;
        final String endDate;
        final int jiraBatchSize;
        final CommandLineParser parser = new DefaultParser();
        final CommandLine commandLineArgs;
        final CustomFieldDefinition[] customFieldDefinitions;
        try {
            commandLineArgs = parser.parse(options, args);
            startDate = commandLineArgs.getOptionValue("start");
            endDate = commandLineArgs.getOptionValue("end");
            jiraBatchSize = Integer.parseInt(commandLineArgs.getOptionValue("jiraBatchSize"));

            final String propFileName = commandLineArgs.getOptionValue("props");
            final PropertiesConfiguration config = new PropertiesConfiguration();
            config.load(propFileName);
            final String jiraUsername = config.getString("jira.username");
            final String jiraPassword = config.getString("jira.password");
            final String jiraBaseUrl = config.getString("jira.baseurl");
            final String[] jiraFieldArray = config.getStringArray("jira.fields");
            final String jiraFields = COMMA_JOINER.join(jiraFieldArray);
            final String jiraExpand = config.getString("jira.expand");
            final String[] jiraProjectArray = config.getStringArray("jira.project");
            final String jiraProject = COMMA_JOINER.join(jiraProjectArray);
            final String[] excludedJiraProjectArray = config.getStringArray("jira.projectexcluded");
            final String excludedJiraProject = COMMA_JOINER.join(excludedJiraProjectArray);
            final String iuploadUrl = config.getString("iupload.url");
            final String iuploadUsername = config.getString("iupload.username");
            final String iuploadPassword = config.getString("iupload.password");
            final String indexName = config.getString("indexname");
            final String customFieldsPath = config.getString("customfieldsfile");
            if(StringUtils.isEmpty(customFieldsPath)) {
                customFieldDefinitions = new CustomFieldDefinition[0];
            } else {
                customFieldDefinitions = CustomFieldDefinitionParser.parseCustomFields(this.getClass().getClassLoader().getResourceAsStream(customFieldsPath));
            }

            final boolean buildSnapshotIndex = config.getBoolean("snapshot.build");
            final int jiraIssuesLookbackMonths = config.getInt("snapshot.lookbackmonths");
            final String snapshotIndexName = config.getString("snapshot.indexname");
            final int snapshotReadRetries = config.getInt("snapshot.read.retries", 5);
            final int snapshotWriteRetries = config.getInt("snapshot.write.retries", 5);
            final String[] deliveryLeadTimeStatuses = config.getStringArray("snapshot.deliveryleadtime..statuses");
            final String[] deliveryLeadTimeResolutions = config.getStringArray("snapshot.deliveryleadtime..resolutions");
            final String[] deliveryLeadTimeTypes = config.getStringArray("snapshot.deliveryleadtime.types");
            final OptionalInt maxStringTermLength = Optional.ofNullable(config.getInteger("index.maxStringTermLength", null))
                    .map(OptionalInt::of).orElse(OptionalInt.empty());

            final JiraActionsIndexBuilderConfig indexBuilderConfig = ImmutableJiraActionsIndexBuilderConfig.builder()
                    .jiraUsername(jiraUsername)
                    .jiraPassword(jiraPassword)
                    .jiraBaseURL(jiraBaseUrl)
                    .jiraFields(jiraFields)
                    .jiraExpand(jiraExpand)
                    .jiraProject(jiraProject)
                    .excludedJiraProject(excludedJiraProject)
                    .iuploadURL(iuploadUrl)
                    .iuploadUsername(iuploadUsername)
                    .iuploadPassword(iuploadPassword)
                    .startDate(startDate)
                    .endDate(endDate)
                    .jiraBatchSize(jiraBatchSize)
                    .indexName(indexName)
                    .buildSnapshotIndex(buildSnapshotIndex)
                    .snapshotLookbackMonths(jiraIssuesLookbackMonths)
                    .snapshotIndexName(snapshotIndexName)
                    .snapshotReadRetries(snapshotReadRetries)
                    .snapshotWriteRetries(snapshotWriteRetries)
                    .deliveryLeadTimeStatuses(new HashSet<>(Arrays.asList(deliveryLeadTimeStatuses)))
                    .deliveryLeadTimeResolutions(new HashSet<>(Arrays.asList(deliveryLeadTimeResolutions)))
                    .deliveryLeadTimeTypes(new HashSet<>(Arrays.asList(deliveryLeadTimeTypes)))
                    .customFields(customFieldDefinitions)
                    .maxStringTermLength(maxStringTermLength)
                    .build();
            indexBuilder = new JiraActionsIndexBuilder(indexBuilderConfig);

        } catch (final ParseException|ConfigurationException|IOException e) {
            LOGGER.error("Failed to initialize builder", e);
            System.exit(-1);
        }
    }

    private void run() {
        try {
            indexBuilder.run();
        } catch (final Exception e) {
            LOGGER.error("Failure running builder", e);
            System.exit(-1);
        }
    }
}
