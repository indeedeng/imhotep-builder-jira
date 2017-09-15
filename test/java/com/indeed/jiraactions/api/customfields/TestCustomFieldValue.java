package com.indeed.jiraactions.api.customfields;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class TestCustomFieldValue {
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
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Story Points")
                .customFieldId("customfield_12090")
                .imhotepFieldName("millistorypoints")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.NONE)
                .transformation(CustomFieldDefinition.Transformation.MULTIPLY_BY_THOUSAND)
                .build();
        final String value = "1.1";
        final CustomFieldValue field = new CustomFieldValue(definition, value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("1100", writer.getBuffer().toString());
    }

    @Test
    public void testExpandedWithChild() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Direct Cause")
                .customFieldId("customfield_17490")
                .imhotepFieldName("evnt_directcause")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.EXPANDED)
                .transformation(CustomFieldDefinition.Transformation.NONE)
                .build();
        final String value = "Parent values: Escaped bug(20664)Level 1 values: Latent Code Issue(20681)";
        final CustomFieldValue field = new CustomFieldValue(definition, value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("Escaped bug - Latent Code Issue", writer.getBuffer().toString());
    }

    @Test
    public void testExpandedWithoutChild() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Direct Cause")
                .customFieldId("customfield_17490")
                .imhotepFieldName("evnt_directcause")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.EXPANDED)
                .transformation(CustomFieldDefinition.Transformation.NONE)
                .build();
        final String value = "Parent values: Escaped bug(20664)";
        final CustomFieldValue field = new CustomFieldValue(definition, value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("Escaped bug", writer.getBuffer().toString());
    }

    @Test
    public void testSeparateWithChild() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Sysad Categories")
                .customFieldId("customfield_17591")
                .imhotepFieldName("sysad_category")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.SEPARATE)
                .transformation(CustomFieldDefinition.Transformation.NONE)
                .build();
        final String value = "Parent values: Misconfiguration(20661)Level 1 values: App Config(20669)";
        final CustomFieldValue field = new CustomFieldValue(definition, value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("Misconfiguration\tApp Config", writer.getBuffer().toString());
    }

    @Test
    public void testSeparateWithoutChild() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Sysad Categories")
                .customFieldId("customfield_17591")
                .imhotepFieldName("sysad_category")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.SEPARATE)
                .transformation(CustomFieldDefinition.Transformation.NONE)
                .build();
        final String value = "Parent values: Misconfiguration(20661)";
        final CustomFieldValue field = new CustomFieldValue(definition, value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("Misconfiguration\t", writer.getBuffer().toString());
    }

    @Test
    public void testSeparateAndTransformed() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Made Up")
                .customFieldId("customfield_00000")
                .imhotepFieldName("fieldName")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.SEPARATE)
                .transformation(CustomFieldDefinition.Transformation.MULTIPLY_BY_THOUSAND)
                .build();
        final String value = "Parent values: 99(32767)Level 1 values: .5(86753)";
        final CustomFieldValue field = new CustomFieldValue(definition, value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("99000\t500", writer.getBuffer().toString());
    }

    @Test
    public void testExpandedAndTransformed() throws IOException {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Made Up")
                .customFieldId("customfield_00000")
                .imhotepFieldName("fieldName")
                .multiValueFieldConfiguration(CustomFieldDefinition.MultiValueFieldConfiguration.EXPANDED)
                .transformation(CustomFieldDefinition.Transformation.MULTIPLY_BY_THOUSAND)
                .build();
        final String value = "Parent values: 9.9(32767)Level 1 values: .5(86753)";
        final CustomFieldValue field = new CustomFieldValue(definition, value);

        final StringWriter writer = new StringWriter();
        field.writeValue(writer);

        Assert.assertEquals("9900 - 500", writer.getBuffer().toString());
    }
}
