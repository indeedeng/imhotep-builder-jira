package com.indeed.jiraactions;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 *
 */
public class TestJiraActionIndexBuilderCommandLineTool {

    @Test
    public void testArrayToCommanDelimetedString() {
        final Map<String, String[]> values = ImmutableMap.<String, String[]>builder()
                .put("", new String[0])
                .put("a,b,c", new String[]{"a", "b", "c"})
                .build();

        for (final Map.Entry<String, String[]> entry : values.entrySet()) {
            final String expected = entry.getKey();
            final String actual = JiraActionsIndexBuilderCommandLineTool.arrayToCommaDelimetedString(entry.getValue());
            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void testNullArrayToCommaDelimetedString() {
        final String expected = "";
        final String actual = JiraActionsIndexBuilderCommandLineTool.arrayToCommaDelimetedString(null);
        Assert.assertEquals(expected, actual);
    }
}
