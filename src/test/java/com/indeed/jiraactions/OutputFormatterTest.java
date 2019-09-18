package com.indeed.jiraactions;

import org.junit.Assert;
import org.junit.Test;

import java.util.OptionalInt;


public class OutputFormatterTest {
    @Test
    public void testFieldTruncationHappyPath() {
        final OutputFormatter outputFormatter = new OutputFormatter(OptionalInt.of(10));
        Assert.assertEquals("abc 123", outputFormatter.truncate("abc 123", " "));
        Assert.assertEquals("", outputFormatter.truncate("", " "));
        Assert.assertEquals(" ", outputFormatter.truncate(" ", " "));
    }

    @Test
    public void testDontTruncateWhenEmpty() {
        final OutputFormatter outputFormatter = new OutputFormatter(OptionalInt.empty());
        Assert.assertEquals("ABC-123 CDE-456789", outputFormatter.truncate("ABC-123 CDE-456789", " "));
    }

    @Test
    public void testOneFieldLongerThanLength() {
        final OutputFormatter outputFormatter = new OutputFormatter(OptionalInt.of(10));
        Assert.assertEquals(OutputFormatter.TRUNCATED_INDICATOR, outputFormatter.truncate("thistextiswaymorethantencharacters", " "));
    }

    @Test
    public void testTruncation() {
        final OutputFormatter outputFormatter = new OutputFormatter(OptionalInt.of(30));
        Assert.assertEquals("ABC-123 XYZ-12345 " + OutputFormatter.TRUNCATED_INDICATOR, outputFormatter.truncate("ABC-123 XYZ-12345 MNO-85675309 ABC-2222", " "));
    }

    @Test
    public void testNoDelimeter() {
        final OutputFormatter outputFormatter = new OutputFormatter(OptionalInt.of(12));
        Assert.assertEquals("1" + OutputFormatter.TRUNCATED_INDICATOR, outputFormatter.truncate("1234567890ABCDEF", null));
        Assert.assertEquals("1" + OutputFormatter.TRUNCATED_INDICATOR, outputFormatter.truncate("1234567890ABCDEF", ""));
    }
}
