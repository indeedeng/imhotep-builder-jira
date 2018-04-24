package com.indeed.jiraactions.api.links;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TestLinkFactory {
    private final LinkFactory factory = new LinkFactory();

    @Before
    public void setup() {
    }

    @Test
    public void testNewLink() {
        final String issuekey = "ABC-123";
        final List<String> types = ImmutableList.<String>builder()
                .add("is referenced by")
                .add("references")
                .add("is duplicated by")
                .add("duplicates")
                .add("is incorporated by")
                .add("incorporates")
                .add("is depended on by")
                .add("depends upon")
                .build();

        for(final String type : types) {
            final String input = String.format("This issue %s %s", type, issuekey);
            final Link actual = factory.makeLink(input);
            Assert.assertEquals(issuekey, actual.getTargetKey());
            Assert.assertEquals(type, actual.getDescription());
        }
    }
}
