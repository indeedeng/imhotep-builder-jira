%default logroot '/var/log'

DEFINE LogLoader com.indeed.squall.pigutil.LogLoader('$logStartTimeStamp', '$logEndTimeStamp');
DEFINE ParseGitCommitSha1 com.indeed.squall.pigutil.logs.ParseLogFieldJustValue('sha1');
DEFINE ParseGitCommitProjectId com.indeed.squall.pigutil.logs.ParseLogFieldJustValue('projectID');
DEFINE ParseGitCommitUid com.indeed.squall.pigutil.logs.ParseLogFieldJustValue('uid');

-- load logentries from logrepo
git_commit_raw = LOAD '$logroot/gitcommit' USING LogLoader AS (uid:chararray, log:chararray);
git_commit_formatted = FOREACH git_commit_raw
                       GENERATE ParseGitCommitSha1(log) as sha1:chararray,
                       ParseGitCommitProjectId(log) as projectId:int,
                       ParseGitCommitUid(log) as uid:chararray;
logs = GROUP git_commit_formatted by sha1;
