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
        final InputStream in = createInputStreamFromDefinitions(TEST_NAME);
        final CustomFieldDefinition[] definitions = CustomFieldDefinitionParser.parseCustomFields(in);
        final StringWriter writer = new StringWriter();

        Assert.assertNotNull(definitions);
        Assert.assertEquals(1, definitions.length);
        Assert.assertEquals(
                ImmutableCustomFieldDefinition.builder()
                        .name("Test Name")
                        .customFieldId("customfield_17591")
                        .imhotepFieldName("protest_name")
                        .build(),
                definitions[0]);

        definitions[0].writeHeader(writer);

        Assert.assertEquals("protest_name", writer.getBuffer().toString());
    }

    @Test
    public void testTransformationMultiplyByThousand() throws IOException {
        final InputStream in = createInputStreamFromDefinitions(STORY_POINTS);
        final CustomFieldDefinition[] definitions = CustomFieldDefinitionParser.parseCustomFields(in);
        final StringWriter writer = new StringWriter();

        Assert.assertNotNull(definitions);
        Assert.assertEquals(1, definitions.length);
        Assert.assertEquals(
                ImmutableCustomFieldDefinition.builder()
                        .name("Story Points")
                        .customFieldId("customfield_12090")
                        .imhotepFieldName("millistorypoints")
                        .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.NONE)
                        .transformation(CustomFieldDefinition.Transformation.MULTIPLY_BY_THOUSAND)
                        .build(),
                definitions[0]);

        definitions[0].writeHeader(writer);

        Assert.assertEquals("millistorypoints", writer.getBuffer().toString());
    }

    @Test
    public void testMultiValueSeparate() throws IOException {
        final InputStream in = createInputStreamFromDefinitions(SYSAD_CATEGORIES);
        final CustomFieldDefinition[] definitions = CustomFieldDefinitionParser.parseCustomFields(in);
        final StringWriter writer = new StringWriter();

        Assert.assertNotNull(definitions);
        Assert.assertEquals(1, definitions.length);
        Assert.assertEquals(
                ImmutableCustomFieldDefinition.builder()
                    .name("Sysad Categories")
                    .customFieldId("customfield_17591")
                    .imhotepFieldName("sysad_category")
                    .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.SEPARATE)
                    .transformation(CustomFieldDefinition.Transformation.NONE)
                    .build(),
                definitions[0]);

        definitions[0].writeHeader(writer);

        Assert.assertEquals("sysad_category1\tsysad_category2", writer.getBuffer().toString());
    }

    @Test
    public void testSeparator() throws IOException {
        final InputStream in = createInputStreamFromDefinitions(TEST_COUNTRIES);
        final CustomFieldDefinition[] definitions = CustomFieldDefinitionParser.parseCustomFields(in);
        final StringWriter writer = new StringWriter();

        Assert.assertNotNull(definitions);
        Assert.assertEquals(1, definitions.length);
        Assert.assertEquals(
                ImmutableCustomFieldDefinition.builder()
                    .name("Test Countries")
                    .customFieldId("customfield_15290")
                    .imhotepFieldName("protest_countries*|")
                    .separator("|")
                    .build(),
                definitions[0]);

        definitions[0].writeHeader(writer);

        Assert.assertEquals("protest_countries*|", writer.getBuffer().toString());
    }

    private InputStream createInputStreamFromDefinitions(final String... elements) {
        final String json = String.format("[\n%s\n]", String.join(",", elements));
        return IOUtils.toInputStream(json);
    }
}
