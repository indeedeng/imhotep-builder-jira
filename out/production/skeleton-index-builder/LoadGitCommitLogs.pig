%default logroot '/var/log'

DEFINE LogLoader com.indeed.squall.pigutil.LogLoader('$logStartTimeStamp', '$logEndTimeStamp');
DEFINE ParseGitCommitSha1 com.indeed.squall.pigutil.logs.ParseLogFieldJustValue('sha1');
DEFINE ParseGitCommitProjectId com.indeed.squall.pigutil.logs.ParseLogFieldJustValue('projectID');

-- load logentries from logrepo
git_commit_raw = LOAD '$logroot/gitcommit' USING LogLoader AS (uid:chararray, log:chararray);
git_commit_formatted = FOREACH git_commit_raw
                       GENERATE
                       (chararray) ParseGitCommitSha1(log) as sha1,
                       (int) ParseGitCommitProjectId(log) as projectId,
                       uid;
logs = GROUP git_commit_formatted by sha1;
