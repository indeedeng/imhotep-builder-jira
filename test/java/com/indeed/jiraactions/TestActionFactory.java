package com.indeed.jiraactions;

import com.indeed.jiraactions.api.customfields.CustomFieldValue;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class TestActionFactory {
    @Test
    public void testNumericStringToMilliNumericString() {
        Assert.assertEquals("", CustomFieldValue.numericStringToMilliNumericString(null));
        Assert.assertEquals("", CustomFieldValue.numericStringToMilliNumericString(""));
        Assert.assertEquals("", CustomFieldValue.numericStringToMilliNumericString("5 cows"));

        Assert.assertEquals("5000", CustomFieldValue.numericStringToMilliNumericString("5"));
        Assert.assertEquals("230", CustomFieldValue.numericStringToMilliNumericString(".23"));
    }
}
