package com.indeed.skeleton.index.builder.gitcommit;

import com.indeed.imhotep.builders.BuilderUtils;
import com.indeed.squall.hadoopcommon.KerberosUtils;
import com.indeed.imhotep.builders.LogJoiner;
import com.indeed.squall.pigutil.PigScriptRunner;
import org.apache.commons.configuration.Configuration;
import org.apache.pig.ExecType;
import org.apache.pig.builtin.BinStorage;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jhamacher
 */
public class GitCommitLogJoiner implements LogJoiner {
    @Override
    public void run(String logRoot, String outputPath, DateTime start, DateTime end, Map<String, String> hadoopConfiguration, Configuration properties) throws IOException {
        KerberosUtils.loginFromKeytab(properties);
        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("logStartTimeStamp", Long.toString(start.getMillis()));
        paramMap.put("logEndTimeStamp", Long.toString(end.getMillis()));
        PigScriptRunner.run(
                hadoopConfiguration,
                BuilderUtils.newResourceSupplier("/LoadGitCommitLogs.pig", getClass()),
                "LoadGitCommitLogs.pig",
                "logs",
                outputPath,
                paramMap,
                BinStorage.class.getName(),
                ExecType.MAPREDUCE
        );
    }
}
