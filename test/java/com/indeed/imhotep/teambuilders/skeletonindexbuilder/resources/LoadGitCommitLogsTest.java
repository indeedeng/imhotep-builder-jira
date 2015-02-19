package com.indeed.imhotep.teambuilders.skeletonindexbuilder.resources;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.Assert.assertEquals;

import com.indeed.imhotep.builders.pigunit.ImhotepPigTestHelper;
import com.indeed.imhotep.teambuilders.skeletonindexbuilder.gitlab.GitLabEntry;
import com.indeed.logging.client.uid.UID;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.junit.Test;

/**
 * @author jhamacher
 */
public class LoadGitCommitLogsTest {

    // This corresponds to the variable assigned from the 'LOAD' statement in
    // the pig file.
    private static final String EVENT_TYPE = "git_commit_raw";

    private static final long START_TIME = System.currentTimeMillis();
    private static final long END_TIME = START_TIME + MILLISECONDS.convert(5, MINUTES);

    private static final ImhotepPigTestHelper helper = ImhotepPigTestHelper.builder()
            .setScriptFile("src/resources/LoadGitCommitLogs.pig")
            .setGeneratedUidTimeRange(START_TIME, END_TIME)
            .setParam("logStartTimeStamp", START_TIME)
            .setParam("logEndTimeStamp", END_TIME)
            // This seems redundant, but is required.
            .overrideAliasWithEventType(EVENT_TYPE, EVENT_TYPE)
            .build();

    @Test
    public void testIndexBuilder() throws Exception {
        helper.clearCustomData();

        final int projectId = 296;
        final String sha1One = "aa2fc2ca51e34a9d004441e82aa3946b497762f7";
        final String sha1Two = "49cb12ca78e80ac7ae96d408adc7e69ad8423f7e";

        final UID uidOne = helper.createEvent(EVENT_TYPE)
                .setParam("projectID", projectId)
                .setParam("sha1", sha1One)
                .setParam("v", 0)
                .finish();

        final UID uidTwo = helper.createEvent(EVENT_TYPE)
                .setParam("projectID", projectId)
                .setParam("sha1", sha1Two)
                .setParam("v", 0)
                .finish();

        final DataBag output = helper.runAndDumpAlias("logs");
        assertEquals(2, output.size());
        assertBagContains(uidOne, sha1One, projectId, output);
        assertBagContains(uidTwo, sha1Two, projectId, output);
    }

    private void assertBagContains(UID uid, String sha1, int projectId, DataBag data) throws Exception {
        for (final Tuple groupedLog : data) {
            if (groupedLog.get(0).equals(uid.toString())) {
                final DataBag bag = (DataBag)groupedLog.get(1);
                assertEquals(1, bag.size());
                for (final Tuple tuple : bag) {
                    assertEquals(3, tuple.size());
                    final GitLabEntry entry = GitLabEntry.parseFrom(tuple);
                    assertEquals(sha1, entry.getSha1());
                    assertEquals(projectId, entry.getProjectId());
                    assertEquals(uid.getTimeStamp(), entry.getTimestampMillis());
                }
            }
        }
    }
}
