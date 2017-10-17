package com.ge.dwt.jenkins

def notifyBuildSlack(String buildStatus, String toChannel) {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  def summary = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (<${env.BUILD_URL}|Jenkins>)"

  // Default values
  def colorCode = '#FF0000'

  if (buildStatus == 'STARTED' || buildStatus == 'UNSTABLE') {
    colorCode = '#FFFF00' // YELLOW
  } else if (buildStatus == 'SUCCESSFUL') {
    colorCode = '#00FF00' // GREEN
  } else {
    colorCode = '#FF0000' // RED
  }

  // Send slack notifications all messages
  slackSend (color: colorCode, message: summary, channel: toChannel)
}

def notifyDeployIssueSlack(String issue, String toChannel) {
  def baseJiraUrl = 'https://jira.franconnect.net/jira'

  def summary = "Deploy Request '${env.JOB_NAME} [${env.BUILD_NUMBER}]' Opened (<${env.BUILD_URL}|Jenkins>) (<${baseJiraUrl}/browse/${issue}|Jira>)"

  def colorCode = '#D3D3D3'

  slackSend (color: colorCode, message: summary, channel: toChannel)
}

def notifyBuildEmail(String buildStatus, String toEmail) {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def details = """<p>${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

  emailext (
    subject: subject,
    body: details,
    mimeType: 'text/html',
    to: toEmail
  )
}

def notifySlackApprovalApplicationOwner(String toChannel) {
  def summary = "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' is awaiting approval from Application Owner (<${env.BUILD_URL}input/|Jenkins>)"

  def colorCode = '#FF9900' // orange

  // Send slack notifications all messages
  slackSend (color: colorCode, message: summary, channel: toChannel)
}

def notifySlackApprovalDevOpsLead() {
  def summary = "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' is awaiting approval from DevOps Lead (<${env.BUILD_URL}input/|Jenkins>)"

  def toChannel = "#Techsol-Devops"

  def colorCode = '#FF9900' // orange

  // Send slack notifications all messages
  slackSend (color: colorCode, message: summary, channel: toChannel)
}

def notifySlackWaitingDeploy() {
  def summary = "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' is awaiting deployment approval (<${env.BUILD_URL}input/|Jenkins>)"

  def toChannel = "#Techsol-Devops"

  def colorCode = '#4169E1' // royalblue

  // Send slack notifications all messages
  slackSend (color: colorCode, message: summary, channel: toChannel)
}

def notifyDeploySlack(String buildStatus, String toChannel) {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  def summary = "${buildStatus}: Deploy '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (<${env.BUILD_URL}|Jenkins>)"

  def colorCode = '#FF0000'

  if (buildStatus == 'STARTED' || buildStatus == 'UNSTABLE') {
    colorCode = '#FFFF00' // YELLOW
  } else if (buildStatus == 'SUCCESSFUL') {
    colorCode = '#008000' // GREEN
  } else {
    colorCode = '#FF0000' // RED
  }

  // Send slack notifications all messages
  slackSend (color: colorCode, message: summary, channel: toChannel)
}
