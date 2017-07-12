package com.indeed.jiraactions;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class TestActionFactory {
    @Test
    public void testNumericStringToMilliNumericString() {
        Assert.assertEquals("", ActionFactory.numericStringToMilliNumericString(null));
        Assert.assertEquals("", ActionFactory.numericStringToMilliNumericString(""));
        Assert.assertEquals("", ActionFactory.numericStringToMilliNumericString("5 cows"));

        Assert.assertEquals("5000", ActionFactory.numericStringToMilliNumericString("5"));
        Assert.assertEquals("230", ActionFactory.numericStringToMilliNumericString(".23"));
    }
}
