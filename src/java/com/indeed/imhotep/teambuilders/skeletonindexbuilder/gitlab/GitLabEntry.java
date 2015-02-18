package com.indeed.imhotep.teambuilders.skeletonindexbuilder.gitlab;

import com.indeed.logging.client.uid.UID;
import org.apache.log4j.Logger;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.Tuple;

/**
 * @author jhamacher
 */
public class GitLabEntry {
    private static final Logger LOG = Logger.getLogger(GitLabEntry.class);
    final private String sha1;
    final private int projectId;
    final private String uid;

    public GitLabEntry(final String sha1, final int projectId, final String uid) {
        this.sha1 = sha1;
        this.projectId = projectId;
        this.uid = uid;
    }

    public static GitLabEntry parseFrom(final Tuple pigTuple) {
        try {
            final String sha1 = (String) pigTuple.get(0);
            final int projectId = Integer.parseInt((String) pigTuple.get(1));
            final String uid = (String) pigTuple.get(2);
            return new GitLabEntry(sha1, projectId, uid);
        } catch (final ExecException e) {
            LOG.error("Failed to create GitLabEntry from tuple: " + pigTuple, e);
        }
        return null;
    }

    public String getSha1() {
        return sha1;
    }

    public int getProjectId() {
        return projectId;
    }

    public long getTimestampMillis() {
        UID uidObject = new UID(uid);
        return uidObject.getTimeStamp();
    }
}
