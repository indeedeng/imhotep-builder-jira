#!/usr/bin/env groovy
@Library('pipeline@release/2.x') _
 
script = this
 
bootstrapIvyUpdater(script) {
    gitConfig.gitUrl = "git@code.corp.indeed.com:delivery/jiraactions.git"
    gitConfig.gitBranch = "master"
    config.newBuildAvailableEmailList = "kbinswanger@indeed.com"
    config.failureEmailList = "kbinswanger@indeed.com"
 
    // Set true if you're going to use buildInDockerContainer
    config.buildInDocker = true
 
    // Set true to enable Slack notifications. (disabled by default)
    slackConfig.enable = false
    // Set Slack notification channel
    slackConfig.channel = "#channel-name"
 
}
 
updateIvyDependencies(
        script,
        "ivy-update"      // Name of branch to use for lockdown
) {
    // Copy the createDeployable block from the main Jenkinsfile (environment and commands to build and test)
    buildInDockerContainer(script) {
        dockerSh("ant clean test test-bundle testability-report distribute")
    }
}
