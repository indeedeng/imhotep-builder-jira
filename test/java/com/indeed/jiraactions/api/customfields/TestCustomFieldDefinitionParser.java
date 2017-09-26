package  com.indeed.jiraactions.api.customfields;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class TestCustomFieldDefinitionParser {
    private static final String STORY_POINTS =   "  {\n" +
            "    \"name\": \"Story Points\",\n" +
            "    \"customfieldid\": \"customfield_12090\",\n" +
            "    \"imhotepfieldname\": \"millistorypoints\",\n" +
            "    \"multivaluefieldconfiguration\": \"none\",\n" +
            "    \"transformation\": \"multiply_by_thousand\"\n" +
            "  }";

    private static final String SYSAD_CATEGORIES = "  {\n" +
            "    \"name\": \"Sysad Categories\",\n" +
            "    \"customfieldid\": \"customfield_17591\",\n" +
            "    \"imhotepfieldname\": \"sysad_category\",\n" +
            "    \"multivaluefieldconfiguration\": \"separate\",\n" +
            "    \"transformation\": \"none\"\n" +
            "  }";

    private static final String TEST_NAME = "  {\n" +
            "    \"name\": \"Test Name\",\n" +
            "    \"customfieldid\": \"customfield_17591\",\n" +
            "    \"imhotepfieldname\": \"protest_name\"\n" +
            "  }";

    private static final String TEST_COUNTRIES = "  {\n" +
            "    \"name\": \"Test Countries\",\n" +
            "    \"customfieldid\": \"customfield_15290\",\n" +
            "    \"imhotepfieldname\": \"protest_countries*|\",\n" +
            "    \"separator\": \"|\"\n" +
            "  }";

    private static final String ISSUE_SIZE_ESTIMATE = "  {\n" +
            "    \"name\": \"Issue Size Estimate\",\n" +
            "    \"customfieldid\": \"customfield_17090\",\n" +
            "    \"imhotepfieldname\": \"issuesizeestimate\",\n" +
            "    \"alternatename\": \"T-Shirt Size Estimate\"\n" +
            "  }";

    private static final String VERIFIER = "  {\n" +
            "    \"name\": \"Verifier\",\n" +
            "    \"customfieldid\": \"customfield_10003\",\n" +
            "    \"imhotepfieldname\": \"verifier\",\n" +
            "    \"multivaluefieldconfiguration\": \"username\"\n" +
            "  }";

    @Test
    public void testProductionConfigs() throws IOException {
        final File directory = new File("src/resources/customfields");
        Assert.assertTrue(directory.exists());

        final File[] configs = directory.listFiles();
        Assert.assertNotNull(configs);
        Assert.assertTrue(configs.length > 0);

        for(final File config : configs) {
            final InputStream in = new FileInputStream(config);
            final CustomFieldDefinition[] definitions = CustomFieldDefinitionParser.parseCustomFields(in);

            Assert.assertNotNull(definitions);
            Assert.assertTrue(definitions.length > 0);
        }
    }

    @Test
    public void testOmittingEnums() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Test Name")
                .customFieldId("customfield_17591")
                .imhotepFieldName("protest_name")
                .build();
        assertComparison(definition, "protest_name", TEST_NAME);
    }

    @Test
    public void testTransformationMultiplyByThousand() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Story Points")
                .customFieldId("customfield_12090")
                .imhotepFieldName("millistorypoints")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.NONE)
                .transformation(CustomFieldDefinition.Transformation.MULTIPLY_BY_THOUSAND)
                .build();
        assertComparison(definition, "millistorypoints", STORY_POINTS);
    }

    @Test
    public void testMultiValueSeparate() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Sysad Categories")
                .customFieldId("customfield_17591")
                .imhotepFieldName("sysad_category")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.SEPARATE)
                .transformation(CustomFieldDefinition.Transformation.NONE)
                .build();
        assertComparison(definition, "sysad_category1\tsysad_category2", SYSAD_CATEGORIES);
    }

    @Test
    public void testSeparator() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Test Countries")
                .customFieldId("customfield_15290")
                .imhotepFieldName("protest_countries*|")
                .separator("|")
                .build();
        assertComparison(definition, "protest_countries*|", TEST_COUNTRIES);
    }

    @Test
    public void testAlternateName() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Issue Size Estimate")
                .customFieldId("customfield_17090")
                .imhotepFieldName("issuesizeestimate")
                .alternateName("T-Shirt Size Estimate")
                .build();
        assertComparison(definition, "issuesizeestimate", ISSUE_SIZE_ESTIMATE);
    }

    @Test
    public void testUsername() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Verifier")
                .customFieldId("customfield_10003")
                .imhotepFieldName("verifier")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.USERNAME)
                .build();
        assertComparison(definition, "verifier\tverifierusername", VERIFIER);
    }

    private void assertComparison(final CustomFieldDefinition expected, final String expectedHeader, final String input) throws IOException {
        final InputStream in = createInputStreamFromDefinitions(input);
        final CustomFieldDefinition[] definitions = CustomFieldDefinitionParser.parseCustomFields(in);
        final StringWriter writer = new StringWriter();

        Assert.assertNotNull(definitions);
        Assert.assertEquals(1, definitions.length);

        final CustomFieldDefinition actual = definitions[0];

        Assert.assertEquals(expected, actual);

        actual.writeHeader(writer);
        Assert.assertEquals(expectedHeader, writer.getBuffer().toString());
    }

    private InputStream createInputStreamFromDefinitions(final String... elements) {
        final String json = String.format("[\n%s\n]", String.join(",", elements));
        return IOUtils.toInputStream(json);
    }
}
