package com.indeed.imhotep.teambuilders.skeletonindexbuilder.gitcommit;

import com.indeed.imhotep.builders.AbstractFlamdexBuilder;
import com.indeed.squall.hadoopflamdex.FlamdexUpdateMapper;

/**
 * @author jhamacher
 */
public class GitCommitFlamdexBuilder extends AbstractFlamdexBuilder {
    @Override
    protected void setup() {
        documentParserClass = GitCommitDocumentParser.class;
        shardIdGenerator = DAILY;
        setProperty(FlamdexUpdateMapper.SKIP_ALLOWED_SHARDS_CHECK, true);
    }
}
