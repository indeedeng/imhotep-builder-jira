The Jira Actions Indexer is a command line tool designed to create an Imhotep index from actions taken in an instance of JIRA.
Each document in an index is a single action (create, update, or comment) taken on an issue. It does this by asking the API for each issue
that could conceivably be part of the given time range, and decomposing that issue into a series of actions. Those actions are then written
to a series of .tsv (tab-separated value) files that are uploaded to an Imhotep shardbuilder.

# Architecture
The Jira Actions Indexer runs in a loop over a series of batches. First it makes a single API call to see how many issues fall into its time
period. It will break this up into batches (normally 25, but it will temporarily scale back batch sizes when it gets rate limited). For each
batch, it will make an API call to get the JSON representation of the issues, decompose them into a series of actions, filter out actions
that don't fall within the period, and write the correct actions to the TSV file for that date. Then it will repeat on the next batch. When
the last batch finishes, it will upload all the TSV files that have data to the Imhotep IUpload URL.

We can't know definitively which issues were modified during a specific time period. There are two values we can use to calibrate: the
create date and the last modified date. So the indexer has to consume many many more issues than it actually needs in order to guarantee it
finds every issue that was actually modified during the period. It will exclude issues that were created after the end date or last modified
before the start date. These are the only issues we know *can't* be modified during the period.

# Understanding the JIRA API Response
One of the quirks of the JIRA API is that a field can show up in two different places. The API response has two sections: it has a "Fields"
section, followed by a History. The Fields section contains a lot of information about what the current state of the issue, and the History
section contains a changelog for all the modifications to the issue. But it's not quite that simple. If an issue was created with a custom
field set, it will show up only in the Fields section only. If it was changed during the lifetime of the issue, it will instead show up in
the History section. The only way you can find out the starting value is to find the first time it was modified in the History section and
see what it was changed from.

These two sections are represented totally differently. The Fields section is formatted JSON. It will also refer to the field by the name
of the field. The History section, by contrast, will use the internal representation (customfield_XXXXX). The History section just has a From,
a string representation of that From, and corresponding To and string representation of that To. The To and To String (or From and From String)
are JIRA's way of handling things like Primary Keys. Imagine a field with a limited number of options (like country). Each of those countries
would have their own ID. So the To field would have the ID, and the ToString field would contain the name of the country. The Jira Actions
Indexer usually (but not always) wants the String representation of the field.

As an example, the Fields section might contain this:
```json
{
  "self": "https://bugs.indeed.com/rest/api/2/customFieldOption/20661",
  "value": "Misconfiguration",
  "id": "20661",
  "child": {
    "self": "https://bugs.indeed.com/rest/api/2/customFieldOption/20669",
    "value": "App Config","id":"20669"
  }
}
```
The History section might instead contain this:
```json
{
  "from": null, 
  "fromString": "", 
  "to": "TODO", 
  "toString": "Parent values: Escaped bug(20664)Level 1 values: Latent Code Issue(20681)"
}
```

This becomes complicated when you've heavily customized JIRA, as we have at Indeed. All the built-in JIRA fields are
represented by POJOs matching the field names that come back from the API. But there are artifacts of several
different attempts to handle custom fields. To make this more complicated, some custom fields are either parsed weirdly or transformed. It's
not as simple as just taking a value from a field and put it somewhere else.

The correct and best way to do this is to define these values in a .json file (see `src/main/resources/customfields/example-custom-fields.json`). We have some examples of older code-based ways of custom field handling in this codebase (TODO: reference examples). We should clean
up the older ways to use the JSON approach instead. Creating a new, simple field should be as easy as including another definition in a .json file.
More complicated fields may required defining new types of transformations. The principle value of this approach is that it lets us support
different custom fields for different JIRA instances.

# To Run Locally
1. Create a file called `imhotep-jira.properties` (see `imhotep-jira-template.properties` for the basic template). Properties are:
    * `jira.username` (required): username for JIRA instance
    * `jira.password` (required): password for JIRA instance
    * `jira.baseurl` (required): Base URL for JIRA instance 
    * `jira.fields` (required): Base fields to use from JIRA. Recommended: `assignee,comment,creator,issuetype,project,status,resolution,summary,reporter,created,category,fixVersions,duedate,components,labels,priority,updated` 
    * `jira.expand` (required): Expand parameter for JIRA issues API. Recommended: `changelog`
    * `jira.project` (optional): Comma-separated list of JIRA project keys to examine. Default of "" (blank) means all projects
    * `jira.projectexcluded` (optional): Comma-separated list of JIRA project keys to omit.
    * `iupload.url` (required): URL to Imhotep iupload instance.
    * `iupload.username` (required): username for Imhotep iupload
    * `iupload.password` (required): password for Imhotep iupload
    * `indexname` (required): name of Imhotep dataset to update (we used to call a dataset an "index")
    * `customfieldsfile` (optional): relative path to custom field definitions, e.g. `customfields/example-custom-fields.json`
2. Invoke `com.indeed.jiraactions.JiraActionsIndexBuilderCommandLine` with arguments : <br>`--start <start time, for example 2016-09-21>`
  <br>`--end <end time, for example 2016-09-22>`
  <br>`--props <path to imhotep-jira.properties>`
  <br>`--jiraBatchSize <batchSize, for example 10 or 25>`
