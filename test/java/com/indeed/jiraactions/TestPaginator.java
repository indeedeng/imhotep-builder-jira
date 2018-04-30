package com.indeed.jiraactions;

import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.api.response.issue.Issue;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestPaginator {
    private final Action defaultAction = ImmutableProxy.createProxy(Action.class);
    private final DateTime start = DateTime.now().minusYears(3);
    private final DateTime mid = start.plusYears(1);
    private final DateTime end = mid.plusYears(1);

    private EasyMockSupport mw;
    private PageProvider provider;

    private final Issue a = new Issue();
    private List<Action> aActions;
    private final Issue b = new Issue();
    private List<Action> bActions;
    private final Issue c = new Issue();
    private List<Action> cActions;
    private final Issue d = new Issue();
    private List<Action> dActions;
    private final Issue e = new Issue();
    private List<Action> eActions;

    @Before
    public void setup() {
        mw = new EasyMockSupport();
        provider = mw.createMock(PageProvider.class);

        a.key = "A";
        b.key = "B";
        c.key = "C";
        d.key = "D";
        e.key = "E";

        aActions = new ArrayList<>();
        bActions = new ArrayList<>();
        cActions = new ArrayList<>();
        dActions = new ArrayList<>();
        eActions = new ArrayList<>();
    }

    @Test
    public void testNewIssue() throws InterruptedException, IOException {
        final Iterable<Issue> page1 = ImmutableList.of(a, b);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page1);
        aActions.add(getCreateAction(a, mid));
        bActions.add(getCreateAction(b, mid.minusHours(1)));

        final List<Action> page1A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a)).andReturn(page1A);
        final List<Action> page1B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b)).andReturn(page1B);

        provider.writeActions(page1A);
        EasyMock.expectLastCall();
        provider.writeActions(page1B);
        EasyMock.expectLastCall();

        EasyMock.expect(provider.hasPage()).andReturn(false);
        provider.reset();
        EasyMock.expectLastCall();

        final Iterable<Issue> page2 = ImmutableList.of(c, a, b);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page2);
        cActions.add(getCreateAction(c, mid.plusMinutes(10)));
        aActions.add(getCommentAction(a, mid.plusMinutes(3)));

        final List<Action> page2C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c)).andReturn(page2C);
        final List<Action> page2A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a)).andReturn(page2A);
        final List<Action> page2B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b)).andReturn(page2B);

        provider.writeActions(EasyMock.eq(ImmutableList.of(cActions.get(0))));
        EasyMock.expectLastCall();
        provider.writeActions(EasyMock.eq(ImmutableList.of(aActions.get(1))));
        EasyMock.expectLastCall();
        provider.writeActions(EasyMock.eq(ImmutableList.of()));
        EasyMock.expectLastCall();

        provider.reset();
        EasyMock.expectLastCall();


        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page2);

        EasyMock.expect(provider.getActions(c)).andReturn(page2C);

        provider.writeActions(EasyMock.eq(ImmutableList.of()));
        EasyMock.expectLastCall();

        provider.reset();
        EasyMock.expectLastCall();

        mw.replayAll();

        final Paginator paginator = new Paginator(provider, start, end);
        paginator.process();

        mw.verifyAll();
    }

    @Test
    public void testNewActionAfterEndDate() throws InterruptedException, IOException {
        final Iterable<Issue> page1 = ImmutableList.of(a, b, c);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page1);
        aActions.add(getCreateAction(a, mid));
        bActions.add(getCreateAction(b, mid.minusHours(1)));
        cActions.add(getCreateAction(c, mid.minusHours(2)));

        final List<Action> page1A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a)).andReturn(page1A);
        final List<Action> page1B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b)).andReturn(page1B);
        final List<Action> page1C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c)).andReturn(page1C);

        provider.writeActions(page1A);
        EasyMock.expectLastCall();
        provider.writeActions(page1B);
        EasyMock.expectLastCall();
        provider.writeActions(page1C);
        EasyMock.expectLastCall();

        EasyMock.expect(provider.hasPage()).andReturn(false);
        provider.reset();
        EasyMock.expectLastCall();


        final Iterable<Issue> page2 = ImmutableList.of(b, a, c);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page2);
        bActions.add(getCommentAction(b, end.plusMinutes(5)));
        aActions.add(getCommentAction(a, mid.plusMinutes(3)));

        final List<Action> page2B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b)).andReturn(page2B);
        final List<Action> page2A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a)).andReturn(page2A);
        final List<Action> page2C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c)).andReturn(page2C);

        provider.writeActions(EasyMock.eq(ImmutableList.of()));
        EasyMock.expectLastCall();
        provider.writeActions(EasyMock.eq(ImmutableList.of(aActions.get(1))));
        EasyMock.expectLastCall();
        provider.writeActions(EasyMock.eq(ImmutableList.of()));
        EasyMock.expectLastCall();

        provider.reset();
        EasyMock.expectLastCall();


        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page2);

        EasyMock.expect(provider.getActions(b)).andReturn(page2B);

        provider.writeActions(EasyMock.eq(ImmutableList.of()));
        EasyMock.expectLastCall();

        provider.reset();
        EasyMock.expectLastCall();

        mw.replayAll();

        final Paginator paginator = new Paginator(provider, start, end);
        paginator.process();

        mw.verifyAll();
    }

    private Action getCreateAction(final Issue issue, final DateTime timestamp) {
        return ImmutableAction.builder()
                .from(defaultAction)
                .action("create")
                .issuekey(issue.key)
                .timestamp(timestamp)
                .build();
    }

    private Action getCommentAction(final Issue issue, final DateTime timestamp) {
        return ImmutableAction.builder()
                .from(defaultAction)
                .action("comment")
                .issuekey(issue.key)
                .timestamp(timestamp)
                .build();
    }
}
