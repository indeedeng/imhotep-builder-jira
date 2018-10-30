package com.indeed.jiraactions.api.links;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestLinkFactory {
    private final LinkFactory factory = new LinkFactory();
    
    @Test
    public void testNewLink() throws IOException {
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
                .add("Child-issue")
                .add("link that looks like an issuekey ABC-234")
                .add("!@!@()*!#()*")
                .build();

        for(final String type : types) {
            final String input = String.format("This issue %s %s", type, issuekey);
            final Link actual = factory.makeLink(input);
            Assert.assertEquals(issuekey, actual.getTargetKey());
            Assert.assertEquals(type, actual.getDescription());
        }
    }
}
