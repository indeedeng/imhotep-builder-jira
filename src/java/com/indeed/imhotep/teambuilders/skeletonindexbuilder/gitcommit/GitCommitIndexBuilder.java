package com.indeed.imhotep.teambuilders.skeletonindexbuilder.gitcommit;

import com.indeed.imhotep.builders.ImhotepIndexBuilderRunner;

import java.io.IOException;

/**
 * @author jhamacher
 */
public class GitCommitIndexBuilder {
    public static void main(String[] args) throws IOException {
          ImhotepIndexBuilderRunner.runTool(
                GitCommitIndexBuilder.class,
                args,
                "gitcommit",
                new GitCommitLogJoiner(),
                new GitCommitFlamdexBuilder()
        );

    }
}