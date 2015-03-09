package com.indeed.imhotep.teambuilders.skeletonindexbuilder.gitlab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author jhamacher
 */
public class GitLabEntryTest {
    @Test
    public void testIt() {
        GitLabEntry unit = new GitLabEntry("aa2fc2ca51e34a9d004441e82aa3946b497762f7", 296, "19eb5digp03h5d2o");
        assertEquals(1424156445209L, unit.getTimestampMillis());
        assertEquals(296, unit.getProjectId());
        assertEquals("aa2fc2ca51e34a9d004441e82aa3946b497762f7", unit.getSha1());
    }
}
