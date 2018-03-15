package com.indeed.jiraactions.api.response.issue.changelog;

import com.indeed.jiraactions.JiraActionsUtil;
import com.indeed.jiraactions.api.response.issue.ImmutableUser;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;

import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;

public class TestChangeLog {
    @Test
    public void testSort() throws ParseException {
        final History a = new History();
        a.author = ImmutableUser.builder()
                .displayName("authorA")
                .name("authorA")
                .key("authorA")
                .build();
        a.created = JiraActionsUtil.parseDateTime("2017-01-01 00:00:00");
        final Item changeA = new Item();
        changeA.field = "fixVersion";
        changeA.fromString = "";
        changeA.toString = "End of Week 2017-01-06";
        a.items = new Item[]{ changeA };

        final History b = new History();
        b.author = ImmutableUser.builder()
                .displayName("authorB")
                .name("authorB")
                .key("authorB")
                .build();
        b.created = JiraActionsUtil.parseDateTime("2017-01-02 01:00:00");
        final Item changeB = new Item();
        changeB.field = "fixVersion";
        changeB.fromString = "End of Week 2017-01-06";
        changeB.toString = "End of Week 2017-01-13";
        b.items = new Item[]{ changeB };

        final History c = new History();
        c.author = ImmutableUser.builder()
                .displayName("authorC")
                .name("authorC")
                .key("authorC")
                .build();
        c.created = JiraActionsUtil.parseDateTime("2017-01-02 12:00:00");
        final Item changeC = new Item();
        changeC.field = "fixVersion";
        changeC.fromString = "End of Week 2017-01-12";
        changeC.toString = "";
        c.items = new Item[]{ changeC };

        final History d = new History();
        d.author = ImmutableUser.builder()
                .displayName("authorD")
                .name("authorD")
                .key("authorD")
                .build();
        d.created = JiraActionsUtil.parseDateTime("2017-02-01 12:00:00");
        final Item changeD = new Item();
        changeD.field = "fixVersion";
        changeD.fromString = "NEXT_DEPLOY";
        changeD.toString = "";
        d.items = new Item[]{ changeD };


        final History[] histories = { c, b, d, a, };
        final ChangeLog changelog = new ChangeLog();
        changelog.histories = histories;

        changelog.sortHistories();

        Assert.assertEquals(a, changelog.histories[0]);
        Assert.assertEquals(b, changelog.histories[1]);
        Assert.assertEquals(c, changelog.histories[2]);
        Assert.assertEquals(d, changelog.histories[3]);
    }
}
