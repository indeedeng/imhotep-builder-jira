package com.indeed.jiraactions;

import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.fields.Field;
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

    private List<Action> aActions;
    private List<Action> bActions;
    private List<Action> cActions;
    private List<Action> dActions;
    private List<Action> eActions;

    @Before
    public void setup() {
        mw = new EasyMockSupport();
        provider = mw.createMock(PageProvider.class);

        aActions = new ArrayList<>();
        bActions = new ArrayList<>();
        cActions = new ArrayList<>();
        dActions = new ArrayList<>();
        eActions = new ArrayList<>();
    }

    private Issue createIssue(final String key) {
        final Issue issue = new Issue();
        issue.key = key;
        issue.fields = new Field();

        return issue;
    }

    @Test
    public void testNewIssue() throws InterruptedException, IOException {
        final Issue a1 = createIssue("A");
        final Issue b1 = createIssue("B");

        final Iterable<Issue> page1 = ImmutableList.of(a1, b1);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page1);
        aActions.add(getCreateAction(a1, mid));
        bActions.add(getCreateAction(b1, mid.minusHours(1)));

        final List<Action> page1A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a1)).andReturn(page1A);
        final List<Action> page1B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b1)).andReturn(page1B);

        provider.writeActions(page1A);
        EasyMock.expectLastCall();
        provider.writeActions(page1B);
        EasyMock.expectLastCall();

        EasyMock.expect(provider.hasPage()).andReturn(false);
        provider.reset();
        EasyMock.expectLastCall();

        final Issue a2 = createIssue("A");
        final Issue b2 = b1;
        final Issue c2 = createIssue("C");

        final Iterable<Issue> page2 = ImmutableList.of(c2, a2, b2);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page2);
        cActions.add(getCreateAction(c2, mid.plusMinutes(10)));
        aActions.add(getCommentAction(a2, mid.plusMinutes(3)));

        final List<Action> page2C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c2)).andReturn(page2C);
        final List<Action> page2A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a2)).andReturn(page2A);
        final List<Action> page2B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b2)).andReturn(page2B);

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

        EasyMock.expect(provider.getActions(c2)).andReturn(page2C);

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
        final Issue a1 = createIssue("A");
        final Issue b1 = createIssue("B");
        final Issue c1 = createIssue("C");

        final Iterable<Issue> page1 = ImmutableList.of(a1, b1, c1);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page1);
        aActions.add(getCreateAction(a1, mid));
        bActions.add(getCreateAction(b1, mid.minusHours(1)));
        cActions.add(getCreateAction(c1, mid.minusHours(2)));

        final List<Action> page1A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a1)).andReturn(page1A);
        final List<Action> page1B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b1)).andReturn(page1B);
        final List<Action> page1C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c1)).andReturn(page1C);

        provider.writeActions(page1A);
        EasyMock.expectLastCall();
        provider.writeActions(page1B);
        EasyMock.expectLastCall();
        provider.writeActions(page1C);
        EasyMock.expectLastCall();

        EasyMock.expect(provider.hasPage()).andReturn(false);
        provider.reset();
        EasyMock.expectLastCall();

        final Issue a2 = createIssue("A");
        final Issue b2 = createIssue("B");
        final Issue c2 = c1;

        final Iterable<Issue> page2 = ImmutableList.of(b2, a2, c2);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page2);
        bActions.add(getCommentAction(b2, end.plusMinutes(5)));
        aActions.add(getCommentAction(a2, mid.plusMinutes(3)));

        final List<Action> page2B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b2)).andReturn(page2B);
        final List<Action> page2A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a2)).andReturn(page2A);
        final List<Action> page2C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c2)).andReturn(page2C);

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

        EasyMock.expect(provider.getActions(b2)).andReturn(page2B);

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
    public void testSkipIssueSecondTime() throws InterruptedException, IOException {
        final Issue a1 = createIssue("A");
        final Issue b1 = createIssue("B");
        final Issue c1 = createIssue("C");
        final Issue d1 = createIssue("D");
        final Issue e1 = createIssue("E");

        final Iterable<Issue> page1_1 = ImmutableList.of(a1, b1);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page1_1);
        aActions.add(getCreateAction(a1, mid));
        bActions.add(getCreateAction(b1, mid.minusHours(1)));

        final Iterable<Issue> page1_2 = ImmutableList.of(c1, d1);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page1_2);
        cActions.add(getCreateAction(c1, mid.minusHours(2)));
        dActions.add(getCreateAction(d1, mid.minusHours(3)));

        final Iterable<Issue> page1_3 = ImmutableList.of(e1);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page1_3);
        eActions.add(getCreateAction(e1, mid.minusHours(4)));

        final List<Action> page1A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a1)).andReturn(page1A);
        final List<Action> page1B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b1)).andReturn(page1B);
        final List<Action> page1C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c1)).andReturn(page1C);
        final List<Action> page1D = new ArrayList<>(dActions);
        EasyMock.expect(provider.getActions(d1)).andReturn(page1D);
        final List<Action> page1E = new ArrayList<>(eActions);
        EasyMock.expect(provider.getActions(e1)).andReturn(page1E);

        provider.writeActions(page1A);
        EasyMock.expectLastCall();
        provider.writeActions(page1B);
        EasyMock.expectLastCall();
        provider.writeActions(page1C);
        EasyMock.expectLastCall();
        provider.writeActions(page1D);
        EasyMock.expectLastCall();
        provider.writeActions(page1E);
        EasyMock.expectLastCall();

        EasyMock.expect(provider.hasPage()).andReturn(false);
        provider.reset();
        EasyMock.expectLastCall();


        final Issue a2 = createIssue("A");
        final Issue b2 = createIssue("B");
        final Issue c2 = createIssue("C");
        final Issue d2 = createIssue("D");
        final Issue e2 = e1;

        // order is A B|C D|E
        final Iterable<Issue> page2_1 = ImmutableList.of(a2, b2);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page2_1);
        aActions.add(getCommentAction(a2, mid.plusMinutes(10)));
        bActions.add(getCommentAction(b2, mid.plusMinutes(9)));

        // c is skipped because it's put back into the front
        // order is now C A|B D|E
        cActions.add(getCommentAction(c2, mid.plusMinutes(15)));

        final Iterable<Issue> page2_2 = ImmutableList.of(b2, d2);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page2_2);
        dActions.add(getCommentAction(d2, mid.plusMinutes(2)));

        final Iterable<Issue> page2_3 = ImmutableList.of(e2);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page2_3);

        final List<Action> page2A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a2)).andReturn(page2A);
        final List<Action> page2B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b2)).andReturn(page2B).times(2); // second time for the second page
        final List<Action> page2D = new ArrayList<>(dActions);
        EasyMock.expect(provider.getActions(d2)).andReturn(page2D);
        final List<Action> page2E = new ArrayList<>(eActions);
        EasyMock.expect(provider.getActions(e2)).andReturn(page2E);

        provider.writeActions(ImmutableList.of(aActions.get(1)));
        EasyMock.expectLastCall();
        provider.writeActions(ImmutableList.of(bActions.get(1)));
        EasyMock.expectLastCall();
        provider.writeActions(ImmutableList.of());
        EasyMock.expectLastCall();
        provider.writeActions(ImmutableList.of(dActions.get(1)));
        EasyMock.expectLastCall();
        provider.writeActions(ImmutableList.of());
        EasyMock.expectLastCall();

        EasyMock.expect(provider.hasPage()).andReturn(false);
        provider.reset();
        EasyMock.expectLastCall();

        final Issue c3 = c2;
        final Issue a3 = a2;

        final Iterable<Issue> page3_1 = ImmutableList.of(c3, a3);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page3_1);

        final List<Action> page3C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c3)).andReturn(page3C);
        final List<Action> page3A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a3)).andReturn(page3A);

        provider.writeActions(ImmutableList.of(cActions.get(1)));
        EasyMock.expectLastCall();
        provider.writeActions(ImmutableList.of());
        EasyMock.expectLastCall();

        provider.reset();
        EasyMock.expectLastCall();

        final Issue c4 = c3;
        final Issue a4 = a3;

        final Iterable<Issue> page4_1 = ImmutableList.of(c3, a3);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page4_1);

        final List<Action> page4C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c4)).andReturn(page4C);

        provider.writeActions(ImmutableList.of());
        EasyMock.expectLastCall();

        provider.reset();
        EasyMock.expectLastCall();

        provider.reset();
        EasyMock.expectLastCall();

        mw.replayAll();
        final Paginator paginator = new Paginator(provider, start, end);
        paginator.process();
        mw.verifyAll();
    }

    @Test
    public void testSkipUpdatedComment() throws InterruptedException, IOException {
        final Issue a1 = createIssue("A");
        final Issue b1 = createIssue("B");
        final Issue c1 = createIssue("C");

        final Iterable<Issue> page1 = ImmutableList.of(a1, b1, c1);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page1);
        aActions.add(getCreateAction(a1, mid));
        bActions.add(getCreateAction(b1, mid.minusHours(1)));
        cActions.add(getCreateAction(c1, mid.minusHours(2)));

        final List<Action> page1A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a1)).andReturn(page1A);
        final List<Action> page1B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b1)).andReturn(page1B);
        final List<Action> page1C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c1)).andReturn(page1C);

        provider.writeActions(page1A);
        EasyMock.expectLastCall();
        provider.writeActions(page1B);
        EasyMock.expectLastCall();
        provider.writeActions(page1C);
        EasyMock.expectLastCall();

        EasyMock.expect(provider.hasPage()).andReturn(false);
        provider.reset();
        EasyMock.expectLastCall();

        final Issue a2 = createIssue("A");
        final Issue b2 = createIssue("B");
        final Issue c2 = c1;

        final Iterable<Issue> page2 = ImmutableList.of(a2, b2, c2);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page2);
        aActions.add(getCommentAction(a2, mid.plusMinutes(5)));
        bActions.add(getCommentAction(b2, mid.plusMinutes(3)));

        final List<Action> page2A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a2)).andReturn(page2A);
        final List<Action> page2B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b2)).andReturn(page2B);
        final List<Action> page2C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c2)).andReturn(page2C);

        provider.writeActions(EasyMock.eq(ImmutableList.of(aActions.get(1))));
        EasyMock.expectLastCall();
        provider.writeActions(EasyMock.eq(ImmutableList.of(bActions.get(1))));
        EasyMock.expectLastCall();
        provider.writeActions(EasyMock.eq(ImmutableList.of()));
        EasyMock.expectLastCall();

        provider.reset();
        EasyMock.expectLastCall();

        final Issue a3 = createIssue("A");
        final Issue b3 = createIssue("B");
        final Issue c3 = c2;

        final Iterable<Issue> page3 = ImmutableList.of(b3, a3, c3);
        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page3);

        // Some action that would update the lastUpdatedTime but not a new action, like updating a comment
        b3.fields.updated = mid.plusMinutes(10);
        aActions.add(getCommentAction(a3, mid.plusMinutes(7)));

        final List<Action> page3B = new ArrayList<>(bActions);
        EasyMock.expect(provider.getActions(b3)).andReturn(page3B);
        final List<Action> page3A = new ArrayList<>(aActions);
        EasyMock.expect(provider.getActions(a3)).andReturn(page3A);
        final List<Action> page3C = new ArrayList<>(cActions);
        EasyMock.expect(provider.getActions(c3)).andReturn(page3C);

        provider.writeActions(EasyMock.eq(ImmutableList.of()));
        EasyMock.expectLastCall();
        provider.writeActions(EasyMock.eq(ImmutableList.of(aActions.get(2))));
        EasyMock.expectLastCall();
        provider.writeActions(EasyMock.eq(ImmutableList.of()));
        EasyMock.expectLastCall();

        provider.reset();
        EasyMock.expectLastCall();

        EasyMock.expect(provider.hasPage()).andReturn(true);
        EasyMock.expect(provider.getPage()).andReturn(page3);

        EasyMock.expect(provider.getActions(b3)).andReturn(page3B);
        EasyMock.expect(provider.getActions(a3)).andReturn(page3A);

        provider.writeActions(EasyMock.eq(ImmutableList.of()));
        EasyMock.expectLastCall();

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
        issue.fields.updated = timestamp;
        return ImmutableAction.builder()
                .from(defaultAction)
                .action("create")
                .issuekey(issue.key)
                .timestamp(timestamp)
                .build();
    }

    private Action getCommentAction(final Issue issue, final DateTime timestamp) {
        issue.fields.updated = timestamp;
        return ImmutableAction.builder()
                .from(defaultAction)
                .action("comment")
                .issuekey(issue.key)
                .timestamp(timestamp)
                .build();
    }
}
