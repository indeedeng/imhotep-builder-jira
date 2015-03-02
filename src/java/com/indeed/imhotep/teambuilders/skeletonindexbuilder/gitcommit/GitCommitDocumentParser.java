package com.indeed.imhotep.teambuilders.skeletonindexbuilder.gitcommit;

import com.google.common.collect.Lists;
import com.indeed.flamdex.writer.FlamdexDocument;
import com.indeed.imhotep.builders.BuilderUtils;
import com.indeed.imhotep.teambuilders.skeletonindexbuilder.gitlab.GitLabEntry;
import com.indeed.squall.hadoopflamdex.FlamdexDocumentParser;
import com.indeed.squall.hadoopflamdex.ParserResult;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DefaultDataBag;
import org.apache.pig.data.Tuple;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author jhamacher
 */
public class GitCommitDocumentParser implements FlamdexDocumentParser<Text, Tuple> {
    private static final Logger LOG = Logger.getLogger(GitCommitDocumentParser.class);

    @Override
    public void initialize(Configuration conf) { /* intentionally empty */ }

    public ParserResult parse(final Text key, final Tuple value) {
        final String sha1;
        List<GitLabEntry> entries = Lists.newArrayList();
        try {
            LOG.info("Parsing Tuple: " + value);

            // We actually have two copies of the sha1 - this one, and the one
            // in the Tuple below.
            sha1 = value.get(0).toString();

            // We expect this to always be set and to always contain a single
            // entry.  Still, sometimes paranoia pays.
            if (value.get(1) != null) {
                final Iterator<Tuple> entryIterator = ((DefaultDataBag) value.get(1)).iterator();
                while (entryIterator.hasNext()) {
                    final Tuple gitLabTuple = entryIterator.next();
                    LOG.info("Creating GitLabEntry from: " + gitLabTuple);
                    final GitLabEntry gitLabEntry = GitLabEntry.parseFrom(gitLabTuple);
                    entries.add(gitLabEntry);
                }
            }
        } catch (final ExecException e) {
            LOG.error("Unable to parse git commit data returned from pig script", e);
            return null;
        }

        if (entries.size() != 1) {
            LOG.error("Expected to find 1 entry per sha1, but found " + entries.size() + " entries for " + sha1);
            return null;
        }

        return buildDocument(entries.get(0));
    }

    private ParserResult buildDocument(final GitLabEntry gitLabEntry) {

        final long timestampMillis = gitLabEntry.getTimestampMillis();

        final FlamdexDocument doc = new FlamdexDocument();
        doc.setStringField("sha1", gitLabEntry.getSha1());
        doc.setIntField("projectId", gitLabEntry.getProjectId());

        // This is necessary to support time functions in IQL.
        doc.setIntField("unixtime", SECONDS.convert(timestampMillis, MILLISECONDS));

        final String shardId = BuilderUtils.getDailyShardId(timestampMillis);
        return new ParserResult(shardId, Collections.singletonList(doc));
    }
}
