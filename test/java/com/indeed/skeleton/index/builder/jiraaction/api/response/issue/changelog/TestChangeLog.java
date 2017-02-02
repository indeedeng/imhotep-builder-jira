package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog;

import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.User;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.Item;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;

public class TestChangeLog {
    @Before
    public void setup() {

    }

    @Test
    public void testSort() throws ParseException {
        final History a = new History();
        a.author = new User();
        a.author.displayName = "authorA";
        a.created = "2017-01-01 00:00:00";
        final Item changeA = new Item();
        changeA.field = "fixVersion";
        changeA.fromString = "";
        changeA.toString = "End of Week 2017-01-06";
        a.items = new Item[]{ changeA };

        final History b = new History();
        b.author = new User();
        b.author.displayName = "authorB";
        b.created = "2017-01-02 00:00:00";
        final Item changeB = new Item();
        changeB.field = "fixVersion";
        changeB.fromString = "End of Week 2017-01-06";
        changeB.toString = "End of Week 2017-01-13";
        b.items = new Item[]{ changeB };

        final History c = new History();
        c.author = new User();
        c.author.displayName = "authorC";
        c.created = "2017-01-02 01:00:00";
        final Item changeC = new Item();
        changeC.field = "fixVersion";
        changeC.fromString = "End of Week 2017-01-12";
        changeC.toString = "";
        c.items = new Item[]{ changeC };


        final History[] histories = { c, b, a };
        final ChangeLog changelog = new ChangeLog();
        changelog.histories = histories;

        changelog.sortHistories();

        Assert.assertEquals(a, changelog.histories[0]);
        Assert.assertEquals(b, changelog.histories[1]);
        Assert.assertEquals(c, changelog.histories[2]);
    }
}
