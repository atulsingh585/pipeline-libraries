@NonCPS
def getPackageVersionsList() {

    def metadataUrl = 'http://10.26.0.57:8081/nexus/content/repositories/tech-ops-QA_branch/rpm/TruOps/maven-metadata.xml'
    def xml = new XmlSlurper().parseText(new URL(metadataUrl).getText())

    def versions = []
    xml.versioning.versions.children().each { versionXml ->
        versions << versionXml.text()
}

    return versions
}


@NonCPS
def getPackageVersions() {
    return getPackageVersionsList().join('\n')
}


    
stage 'Select Package'
node{

    def version_input = input(message: 'Select package version to deploy', submitterParameter: 'submitter', parameters: [[$class: 'ChoiceParameterDefinition', choices:   getPackageVersions(), description: 'Version of package to Deploy', name: 'Version'],[$class: 'TextParameterDefinition', description: 'Additional Comments', name: 'Comments']])
    def artifact_version = version_input['Version']

       sh """\
#!/bin/bash

set -ex
export JAVA_HOME=/usr/java/jdk1.8.0_101

wget -v --content-disposition "http://10.26.0.57:8081/nexus/content/repositories/tech-ops-QA_branch/rpm/TruOps/${artifact_version}/TruOps-${artifact_version}.rpm"

/home/jenkins/maven/bin/mvn deploy:deploy-file \\
  -DgroupId=rpm \\
  -DartifactId=TruOps \\
  -Dversion=${artifact_version} \\
  -Dpackaging=rpm \\
  -DrepositoryId=nexus \\
  -DgeneratePom=true \\
  -Dfile=./TruOps-${artifact_version}.rpm \\
  -Durl=http://10.26.0.57:8081/nexus/content/repositories/tech-ops-master
"""
} 


 stage 'App Owner Approval'
 node{
 input message:'Approve deployment?', submitter: 'atul'
 }
 
  stage 'Deployment Approval'
 node{
 input message:'Approve deployment?', submitter: 'sanish'
 }
 
 
  stage 'DevOps Approval'
 node{
 input message:'Approve deployment?', submitter: 'devopsadmin'
 }
 
  stage 'Deploy'
  node {
    sh '''\
    set -e 
knife ssh name:PreProd-Node -x root -P Noida@123 "sudo chef-client" 
    
      '''}

 
 