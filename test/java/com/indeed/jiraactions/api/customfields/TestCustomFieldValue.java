package com.indeed.jiraactions.api.customfields;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class TestCustomFieldValue {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final CustomFieldDefinition directCause = ImmutableCustomFieldDefinition.builder()
            .name("Direct Cause")
            .customFieldId("customfield_17490")
            .imhotepFieldName("evnt_directcause")
            .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.EXPANDED)
            .transformation(CustomFieldDefinition.Transformation.NONE)
            .build();

    private static final CustomFieldDefinition storyPoints = ImmutableCustomFieldDefinition.builder()
            .name("Story Points")
            .customFieldId("customfield_12090")
            .imhotepFieldName("millistorypoints")
            .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.NONE)
            .transformation(CustomFieldDefinition.Transformation.MULTIPLY_BY_THOUSAND)
            .build();

    private static final CustomFieldDefinition sysadCategories = ImmutableCustomFieldDefinition.builder()
            .name("Sysad Categories")
            .customFieldId("customfield_17591")
            .imhotepFieldName("sysad_category")
            .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.SEPARATE)
            .transformation(CustomFieldDefinition.Transformation.NONE)
            .build();

    private static final CustomFieldDefinition protestCountries = ImmutableCustomFieldDefinition.builder()
            .name("Test Countries")
            .customFieldId("customfield_15290")
            .imhotepFieldName("protest_countries*|")
            .separator("|")
            .build();

    @Test
    public void testNoModifications() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .customFieldId("customFieldId")
                .imhotepFieldName("imhotepFieldName")
                .name("name")
                .build();
        final String value = "TestNoTransformationsValue";
        final CustomFieldValue field = new CustomFieldValue(definition, value);

        assertEquals(field, value);
    }

    @Test
    public void testMultiplyByThousand() throws IOException {
        testFromInitial(storyPoints, "1.1", "1100");
    }

    @Test
    public void testTextFromInitial() throws IOException {
        testFromInitial(storyPoints, "8.0", "8000");
    }

    @Test
    public void testValueFromInitial() throws IOException {
        testFromInitial(protestCountries, "{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/12062\",\"value\":\"User\",\"id\":\"12062\"}", "User");
    }

    @Test
    public void testSingleValueArrayOfValuesFromInitial() throws IOException {
        testFromInitial(protestCountries, "[{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/16473\",\"value\":\"en only\",\"id\":\"16473\"}]", "en only");
    }

    @Test
    public void testArrayOfValuesFromInitialWithoutSeparator() throws IOException {
        final CustomFieldDefinition madeup = ImmutableCustomFieldDefinition.builder()
                .name("Fake Test Countries")
                .customFieldId("customfield_15290")
                .imhotepFieldName("fake_protest_countries*")
                .build();

        testFromInitial(madeup, "[{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/16473\",\"value\":\"en_only\",\"id\":\"16473\"},{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/16475\",\"value\":\"worldwide\",\"id\":\"16475\"}]",
                "en_only worldwide");
    }

    @Test
    public void testArrayOfValuesFromInitialWithSeparator() throws IOException {
        testFromInitial(protestCountries, "[{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/16473\",\"value\":\"en only\",\"id\":\"16473\"},{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/16475\",\"value\":\"worldwide\",\"id\":\"16475\"}]",
                "en only|worldwide");
    }

    @Test
    public void testArrayOfTextFromInitial() throws IOException {
        final CustomFieldDefinition madeup = ImmutableCustomFieldDefinition.builder()
                .name("Labels")
                .customFieldId("customfield_00000")
                .imhotepFieldName("labels*")
                .build();

        testFromInitial(madeup, "[\"fixit\",\"jobsearch-library-update\",\"jsgrowth\"]", "fixit jobsearch-library-update jsgrowth");
    }

    @Test
    public void testExpandedWithChildFromInitial() throws IOException {
        testFromInitial(directCause, "{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/20661\",\"value\":\"Misconfiguration\",\"id\":\"20661\",\"child\":{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/20669\",\"value\":\"App Config\",\"id\":\"20669\"}}",
                "Misconfiguration - App Config");
    }

    @Test
    public void testExpandedNoChildFromInitial() throws IOException {
        testFromInitial(directCause, "{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/20661\",\"value\":\"Misconfiguration\",\"id\":\"20661\"}", "Misconfiguration");
    }

    @Test
    public void testSeparateWithChildFromInitial() throws IOException {
        testFromInitial(sysadCategories, "{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/20781\",\"value\":\"Config\",\"id\":\"20781\",\"child\":{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/20814\",\"value\":\"Other\",\"id\":\"20814\"}}",
                "Config\tOther");
    }

    @Test
    public void testSeparateWithoutChildFromInitial() throws IOException {
        testFromInitial(sysadCategories, "{\"self\":\"https://bugs.indeed.com/rest/api/2/customFieldOption/20787\",\"value\":\"DNS\",\"id\":\"20787\"}", "DNS\t");
    }

    @Test
    public void testExpandedWithChildFromChangelog() throws IOException {
        final String value = "Parent values: Escaped bug(20664)Level 1 values: Latent Code Issue(20681)";
        final CustomFieldValue field = CustomFieldValue.customFieldValueFromChangelog(directCause, "", value);

        assertEquals(field, "Escaped bug - Latent Code Issue");
    }

    @Test
    public void testExpandedWithoutChildFromChangelog() throws IOException {
        final String value = "Parent values: Escaped bug(20664)";
        final CustomFieldValue field = CustomFieldValue.customFieldValueFromChangelog(directCause, "", value);

        assertEquals(field, "Escaped bug");
    }

    @Test
    public void testSeparateWithChildFromChangelog() throws IOException {
        final String value = "Parent values: Misconfiguration(20661)Level 1 values: App Config(20669)";
        final CustomFieldValue field = CustomFieldValue.customFieldValueFromChangelog(sysadCategories, "", value);

        assertEquals(field, "Misconfiguration\tApp Config");
    }

    @Test
    public void testSeparateWithoutChildFromChangelog() throws IOException {
        final String value = "Parent values: Misconfiguration(20661)";
        final CustomFieldValue field = CustomFieldValue.customFieldValueFromChangelog(sysadCategories, "", value);

        // This tab is important because we need the empty space for the field that isn't present

        assertEquals(field, "Misconfiguration\t");
    }

    @Test
    public void testSeparateAndTransformedFromChangelog() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Made Up")
                .customFieldId("customfield_00000")
                .imhotepFieldName("fieldName")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.SEPARATE)
                .transformation(CustomFieldDefinition.Transformation.MULTIPLY_BY_THOUSAND)
                .build();
        final String value = "Parent values: 99(32767)Level 1 values: .5(86753)";
        final CustomFieldValue field = CustomFieldValue.customFieldValueFromChangelog(definition, "", value);

        assertEquals(field, "99000\t500");
    }

    @Test
    public void testExpandedAndTransformedFromChangelog() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Made Up")
                .customFieldId("customfield_00000")
                .imhotepFieldName("fieldName")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.EXPANDED)
                .transformation(CustomFieldDefinition.Transformation.MULTIPLY_BY_THOUSAND)
                .build();
        final String value = "Parent values: 9.9(32767)Level 1 values: .5(86753)";
        final CustomFieldValue field = CustomFieldValue.customFieldValueFromChangelog(definition, "", value);

        assertEquals(field, "9900 - 500");
    }

    private void testFromInitial(final CustomFieldDefinition definition, final String input, final String expected) throws IOException {
        final JsonNode node = OBJECT_MAPPER.readTree(input);
        final CustomFieldValue field = CustomFieldValue.customFieldFromInitialFields(definition, node);

        assertEquals(field, expected);
    }

    private void assertEquals(final CustomFieldValue field, final String expected) throws IOException {
        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals(expected, writer.getBuffer().toString());
    }
}
