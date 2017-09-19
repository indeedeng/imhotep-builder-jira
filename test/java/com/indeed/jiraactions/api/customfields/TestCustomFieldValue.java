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

    @Test
    public void testNoModifications() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .customFieldId("customFieldId")
                .imhotepFieldName("imhotepFieldName")
                .name("name")
                .build();
        final String value = "TestNoTransformationsValue";
        final CustomFieldValue field = new CustomFieldValue(definition, value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals(value, writer.getBuffer().toString());
    }

    @Test
    public void testMultiplyByThousand() throws IOException {
        final String value = "1.1";
        final CustomFieldValue field = new CustomFieldValue(storyPoints, value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("1100", writer.getBuffer().toString());
    }

    @Test
    public void testNotExpandedFromInitial() throws IOException {
        final String text = "8.0";
        final JsonNode node = OBJECT_MAPPER.readTree(text);
        final CustomFieldValue field = CustomFieldValue.customFieldFromInitialFields(storyPoints, node);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("8000", writer.getBuffer().toString());
    }

    @Test
    public void testExpandedWithChildFromInitial() throws IOException {
        final String text = "{\"self\":\"https://***REMOVED***/rest/api/2/customFieldOption/20661\",\"value\":\"Misconfiguration\",\"id\":\"20661\",\"child\":{\"self\":\"https://***REMOVED***/rest/api/2/customFieldOption/20669\",\"value\":\"App Config\",\"id\":\"20669\"}}";
        final JsonNode node = OBJECT_MAPPER.readTree(text);
        final CustomFieldValue field = CustomFieldValue.customFieldFromInitialFields(directCause, node);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("Misconfiguration - App Config", writer.getBuffer().toString());
    }

    @Test
    public void testExpandedNoChildFromInitial() throws IOException {
        final String text = "{\"self\":\"https://***REMOVED***/rest/api/2/customFieldOption/20661\",\"value\":\"Misconfiguration\",\"id\":\"20661\"}";
        final JsonNode node = OBJECT_MAPPER.readTree(text);
        final CustomFieldValue field = CustomFieldValue.customFieldFromInitialFields(directCause, node);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("Misconfiguration", writer.getBuffer().toString());
    }

    @Test
    public void testSeparateWithChildFromInitial() throws IOException {
        final String text = "{\"self\":\"https://***REMOVED***/rest/api/2/customFieldOption/20781\",\"value\":\"Config\",\"id\":\"20781\",\"child\":{\"self\":\"https://***REMOVED***/rest/api/2/customFieldOption/20814\",\"value\":\"Other\",\"id\":\"20814\"}}";
        final JsonNode node = OBJECT_MAPPER.readTree(text);
        final CustomFieldValue field = CustomFieldValue.customFieldFromInitialFields(sysadCategories, node);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("Config\tOther", writer.getBuffer().toString());
    }

    @Test
    public void testSeparateWithoutChildFromInitial() throws IOException {
        final String text = "{\"self\":\"https://***REMOVED***/rest/api/2/customFieldOption/20787\",\"value\":\"DNS\",\"id\":\"20787\"}";
        final JsonNode node = OBJECT_MAPPER.readTree(text);
        final CustomFieldValue field = CustomFieldValue.customFieldFromInitialFields(sysadCategories, node);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("DNS\t", writer.getBuffer().toString());
    }

    @Test
    public void testExpandedWithChildFromChangelog() throws IOException {
        final String value = "Parent values: Escaped bug(20664)Level 1 values: Latent Code Issue(20681)";
        final CustomFieldValue field = CustomFieldValue.customFieldValueFromChangelog(directCause, "", value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("Escaped bug - Latent Code Issue", writer.getBuffer().toString());
    }

    @Test
    public void testExpandedWithoutChildFromChangelog() throws IOException {
        final String value = "Parent values: Escaped bug(20664)";
        final CustomFieldValue field = CustomFieldValue.customFieldValueFromChangelog(directCause, "", value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("Escaped bug", writer.getBuffer().toString());
    }

    @Test
    public void testSeparateWithChildFromChangelog() throws IOException {
        final String value = "Parent values: Misconfiguration(20661)Level 1 values: App Config(20669)";
        final CustomFieldValue field = CustomFieldValue.customFieldValueFromChangelog(sysadCategories, "", value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("Misconfiguration\tApp Config", writer.getBuffer().toString());
    }

    @Test
    public void testSeparateWithoutChildFromChangelog() throws IOException {
        final String value = "Parent values: Misconfiguration(20661)";
        final CustomFieldValue field = CustomFieldValue.customFieldValueFromChangelog(sysadCategories, "", value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        // This tab is important because we need the empty space for the field that isn't present
        Assert.assertEquals("Misconfiguration\t", writer.getBuffer().toString());
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

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("99000\t500", writer.getBuffer().toString());
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

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("9900 - 500", writer.getBuffer().toString());
    }
}
