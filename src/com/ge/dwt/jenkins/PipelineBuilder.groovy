package com.ge.dwt.jenkins

import com.ge.dwt.jenkins.Nexus
import com.ge.dwt.jenkins.Notifications

def generateProdDeployPipeline(Map settings) {
    def requiredFields = ['jenkins', 'artifactid', 'fileext']

    def missingFields = []

    for (int i = 0; i < requiredFields.size(); i++) {
        def field = requiredFields[i]
        if (!settings.containsKey(field)) {
            missingFields.add(field)
        }
    }
    if (missingFields.size() > 0) {
        throw new java.security.InvalidParameterException("generateProdDeployPipeline() missing field(s): " + missingFields.join(', '))
    }

    def nexusHelper = new Nexus()
    def notificationsHelper = new Notifications()

    def stagerepo = 'MBE-Stage'
    def preprodrepo = 'MBE-PreProd'
    def prodrepo = 'MBE-Prod'
    def groupid = 'rpm'

    def jenkins = settings['jenkins']
    def artifactid
    if (settings['artifactid'] instanceof List) {
        artifactid = settings['artifactid']
    } else {
        artifactid = [ settings['artifactid'] ]
    }
    def appapprover = settings['appapprover']
    def fileext = settings['fileext']
    def appslack = settings['appslack']

    stage 'Select Package'

    def versions = nexusHelper.getPackageVersions(
        repoid: stagerepo,
        groupid: groupid,
        artifactid: artifactid[0]
    )

    def version_input = input(message: 'Select package version to deploy', submitterParameter: 'submitter', parameters: [[$class: 'ChoiceParameterDefinition', choices: versions, description: 'Version of package to Deploy', name: 'Version'],[$class: 'TextParameterDefinition', description: 'Additional Comments', name: 'Comments']])
    def artifact_version = version_input['Version']
    def submitter = version_input['submitter']
    def comments = version_input['Comments']

    def issue

    try {
        issue = jiraHelper.createDeployIssue(artifactid: artifactid.join(","), version: artifact_version, submitter: submitter, comments: comments)
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException ex) {
       // job was cancelled or something, rethrow
       throw ex

    } catch (err) {
       // All other exceptions should be ignored, just mark as unstable
       currentBuild.result = 'UNSTABLE'
       echo("There was an error creating a Jira Issue: " + err.getMessage())
    }

    try {
        notificationsHelper.notifyDeployIssueSlack(issue, '#devops-notifier')
        if (appslack != null) {
            notificationsHelper.notifyDeploySlack(issue, appslack)
        }
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException ex) {
       // job was cancelled or something, rethrow
       throw ex
    } catch (err) {
       // All other exceptions should be ignored, just mark as unstable
       currentBuild.result = 'UNSTABLE'
       echo("There was an error sending a Slack notification: " + err.getMessage())
    }

    stage 'Stage Package'

    node(jenkins) {
        deleteDir() // clean workspace
        for (int i=0; i < artifactid.size(); i++) {
            nexusHelper.copyPackageRepos(
                groupid: groupid,
                artifactid: artifactid[i],
                fileext: fileext,
                version: artifact_version,
                srcrepo: stagerepo,
                dstrepo: preprodrepo
            )
        }
    }

    stage 'App Owner Approval'

    try {
        if (appslack != null) {
            notificationsHelper.notifySlackApprovalApplicationOwner(appslack)
        }
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException ex) {
       // job was cancelled or something, rethrow
       throw ex
    } catch (err) {
       // All other exceptions should be ignored, just mark as unstable
       currentBuild.result = 'UNSTABLE'
       echo("There was an error sending a Slack notification: " + err.getMessage())
    }

    def appApproverResult
    if (appapprover != null) {
        appApproverResult = input message: 'Approve Production Deploy? (App Owner)', ok: 'Approve', submitterParameter: 'submitter', submitter: appapprover
    } else {
        appApproverResult = input message: 'Approve Production Deploy? (App Owner)', ok: 'Approve', submitterParameter: 'submitter'
    }

    try {
        if (issue != null) {
            jiraHelper.addIssueComment(issue: issue, comment: "Deployment Approved by App Owner: ${appApproverResult}")
        }
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException ex) {
       // job was cancelled or something, rethrow
       throw ex
    } catch (err) {
       // All other exceptions should be ignored, just mark as unstable
       currentBuild.result = 'UNSTABLE'
       echo("There was an error commenting on a Jira Issue: " + err.getMessage())
    }

    stage 'Deployment Approval'

    try {
        notificationsHelper.notifySlackApprovalDevOpsLead()
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException ex) {
       // job was cancelled or something, rethrow
       throw ex
    } catch (err) {
       // All other exceptions should be ignored, just mark as unstable
       currentBuild.result = 'UNSTABLE'
       echo("There was an error sending a Slack notification: " + err.getMessage())
    }

    def devopsLeadApproverResult = input message: 'Approve Production Deploy? (DevOps Lead)', ok: 'Approve', submitterParameter: 'submitter', submitter: 'g01132823' // @Digital DWT DevOps Approvers

    try {
        if (issue != null) {
            jiraHelper.addIssueComment(issue: issue, comment: "Deployment Approved by DevOps Lead: ${devopsLeadApproverResult}")
        }
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException ex) {
       // job was cancelled or something, rethrow
       throw ex
    } catch (err) {
       // All other exceptions should be ignored, just mark as unstable
       currentBuild.result = 'UNSTABLE'
       echo("There was an error commenting on a Jira Issue: " + err.getMessage())
    }

    stage 'DevOps Approval'

    try {
        notificationsHelper.notifySlackWaitingDeploy()
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException ex) {
       // job was cancelled or something, rethrow
       throw ex
    } catch (err) {
       // All other exceptions should be ignored, just mark as unstable
       currentBuild.result = 'UNSTABLE'
       echo("There was an error sending a Slack notification: " + err.getMessage())
    }

    def devopsDeployApproverResult = input message: 'Approve Production Deploy? (DevOps)', ok: 'Approve', submitterParameter: 'submitter', submitter: 'g00853193' // @CORP Tech Solutions DevOps

    try {
        if (issue != null) {
            jiraHelper.addIssueComment(issue: issue, comment: "Deployment started by ${devopsDeployApproverResult}")
            jiraHelper.assignIssue(issue: issue, assignee: devopsDeployApproverResult)
        }
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException ex) {
       // job was cancelled or something, rethrow
       throw ex
    } catch (err) {
       // All other exceptions should be ignored, just mark as unstable
       currentBuild.result = 'UNSTABLE'
       echo("There was an error commenting on a Jira Issue: " + err.getMessage())
    }

    stage 'Publish'

    try {
        node(jenkins) {
            deleteDir() // clean workspace

            for (int i=0; i < artifactid.size(); i++) {
                nexusHelper.copyPackageRepos(
                    groupid: groupid,
                    artifactid: artifactid[i],
                    fileext: fileext,
                    version: artifact_version,
                    srcrepo: preprodrepo,
                    dstrepo: prodrepo
                )
            }
        }

        stage 'Deploy'

        node(jenkins) {

        }
    } catch (e) {
        currentBuild.result = "FAILED"
        throw e
    } finally {
        notificationsHelper.notifyDeploySlack(currentBuild.result, "#devops-notifier")
        jiraHelper.completeDeployment(issue: issue, result: currentBuild.result)
        if (appslack != null) {
            notificationsHelper.notifyDeploySlack(currentBuild.result, appslack)
        }
    }
}

def generateRollbackPipeline(Map settings) {
//    def requiredFields = ['jenkins', 'repoid', 'artifactid', 'fileext', 'chefenv', 'rollbackcmd']
    def requiredFields = ['jenkins', 'repoid', 'fileext','artifactid']

    def missingFields = []
    for (int i = 0; i < requiredFields.size(); i++) {
        def field = requiredFields[i]
        if (!settings.containsKey(field)) {
            missingFields.add(field)
        }
    }
    if (missingFields.size() > 0) {
        throw new java.security.InvalidParameterException("generateRollbackPipeline() missing field(s): " + missingFields.join(', '))
    }


    def nexusHelper = new Nexus()

    def groupid = 'rpm'

    def jenkins = settings['jenkins']
    def repoid = settings['repoid']
    def artifactid
    if (settings['artifactid'] instanceof List) {
        artifactid = settings['artifactid']
    } else {
        artifactid = [ settings['artifactid'] ]
    }
    def fileext = settings['fileext']
    def appslack = settings['appslack']
    def appapprover = settings['appapprover']
    def rollbackcmd = settings['rollbackcmd']

    stage 'Select Package'

    def versions = nexusHelper.getPackageVersions(
        repoid: repoid,
        groupid: groupid,
        artifactid: artifactid[0]
    )

    def version_input = input message: 'Select target version for rollback', submitterParameter: 'submitter', parameters: [[$class: 'ChoiceParameterDefinition', choices: versions, description: 'Version of RPM to Rollback To', name: 'Version']]
    def artifact_version = version_input['Version']
    def submitter = version_input['submitter']
    def issue

    stage 'DevOps Approval'

    def devopsRollbackApproverResult = input message: "Approve Production Rollback to ${artifact_version}? (DevOps)", ok: 'Approve', submitterParameter: 'submitter', submitter: 'g00853193' // @CORP Tech Solutions DevOps

    try {
        stage 'Rollback Nexus'

        for (int i=0; i < artifactid.size(); i++) {
            node(jenkins) {
                nexusHelper.artifactRollback(
                    repoid: repoid,
                    groupid: groupid,
                    artifactid: artifactid[i],
                    version: artifact_version
                )
            }
        }

        stage 'Rollback Servers'
        node(jenkins) {
            
        }
    } catch (e) {
        currentBuild.result = "FAILED"
        throw e
    } finally {
        
    }
}
