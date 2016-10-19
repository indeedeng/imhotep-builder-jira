# To Run Locally
1. Create a file called "jirapassword.properties". It should have two fields: jira.username.indexer and jira.password.indexer. Set those to your JIRA account. This file is added to .gitignore, so you shouldn't able to accidentally commit it.
2. Set VM Options: ```"-Dindeed.staging.level=dev -Dindeed.dc=dev -Dindeed.application=JiraActionIndexBuilderCommandLineTool -Dindeed.instance=JiraActionIndexBuilderCommandLineTool  -Dindeed.product.group=jobsearch"```
3. Set Program Options: ```"--start <start time, for example 2016-09-21> --end <end time, for example 2016-09-22> --props <path to jirapassword.properties from part 1, for example /home/kbinswanger/indeed/jiraactions/jirapassword.properties>"


# Setup

## Getting Access to the Hadoop Cluster

To perform any testing, you'll need access to the Hadoop cluster.  Access is controlled via Kerberos.

1. Ensure that the command-line Kerberos tools are installed on your system (`which kinit` should tell you if this is the case).  On Ubuntu, you can get them via `sudo apt-get install krb5-user`.
2. Create or overwrite `/etc/krb5.conf` so that it matches [this example](https://eng-git.ausoff.indeed.net/squall/pigutil/blob/master/setup/cdh4/krb5.conf).
3. All new devs should have kerberos access by default. So you should be able to run 'kinit' successfully now. Else file a ticket requesting kerberos access. You can use [this ticket](https://bugs.indeed.com/browse/SYSAD-22100) as an example.

At this point, you should be able to run 'kinit' successfully.

## Installing Hadoop Locally

You won't actually need to run Hadoop locally, but you will need all of the libraries and configuration files.  I basically followed [this page](https://wiki.indeed.com/display/~veeresh/Installing+CDH5+on+Ubuntu+14.04).
(There's a [old page](https://wiki.indeed.com/display/eng/CDH4+set+up+for+Mac+OSX) for OS X, we don't use CDH4 anymore so this may not work.)
Also check this [CDH5 Migration wiki page](https://wiki.indeed.com/display/eng/CDH5+and+Indeed+v3+Stack+Migration+Instructions). It has lot of useful information.

# Creating a New Index Builder

Obviously you can use this skeleton as a starting point; that's the reason it exists.  Other good sources of information include:
* The [ITBJS guide](https://wiki.indeed.com/pages/viewpage.action?pageId=60625603) to testing and running an index builder.  This was my primary resource.
* [This guide](https://wiki.indeed.com/display/SrchQual/Imhotep+Builder+Guide) goes into some depth about what the individual components do and how to implement them.
* An extremely helpful [guide to unit-testing pig scripts](https://wiki.indeed.com/display/eng/2015/01/09/Unit+Testing+Imhotep+Pig+Scripts).

If you [search GitLab for "imhotep"](https://eng-git.ausoff.indeed.net/search?utf8=%E2%9C%93&search=imhotep&group_id=&repository_ref=), you can discover many different index builders to use for reference.
I found these two particularly useful:
* [Delivery](https://eng-git.ausoff.indeed.net/delivery/imhotep-teambuilders-delivery), which indexes data in both LogRepo and HBase.  It also has some standalone tools for getting data into HBase.
* [Devops](https://eng-git.ausoff.indeed.net/devops/imhotep-teambuilders-devops), which has a ton of unit tests.

Other useful tools:
* [LogViewer](https://squall.indeed.com/logviewer/) allows you to view the logs you're trying to parse, if you're setting up a LogRepo job.

# Running

Compile and package the index builder code using this command:
```bash
ant distribute-hadoop
```

To run this index builder, I used this command:
```bash
java -Djava.security.krb5.conf=/etc/krb5.conf -Dindeed.application=SkeletonIndexBuilder -Dindeed.instance=SkeletonIndexBuilder -cp "/etc/hadoop/conf:/etc/hbase/conf:dist/hadoop-skeleton-index-builder.jar" com.indeed.imhotep.teambuilders.skeletonindexbuilder.gitcommit.GitCommitIndexBuilder --output /var/imhotep-qa --start "2015-02-01T00:00:00" --end "2015-02-01T01:00"
```

You can view the resulting index [here](https://squall.indeed.com/iqlweb/#q[]=from+gitcommit+2015-02-01+2015-02-02&backend=qa&view=table).

# Next Steps

You'll probably want to [set up an Orc job](https://wiki.indeed.com/display/SYSADMIN/Adding+a+new+process+to+Orc) automate building your index.
