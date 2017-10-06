package com.ge.dwt.jenkins

@NonCPS
def getPackageVersionsList(Map settings) {
    def requiredFields = ['repoid', 'groupid', 'artifactid']

    def missingFields = []
    requiredFields.each { field ->
        if (!settings.containsKey(field)) {
            missingFields.add(field)
        }
    }
    if (missingFields.size() > 0) {
        throw new java.security.InvalidParameterException("getPackageVersions() missing field(s): " + missingFields.join(', '))
    }

    def metadataUrl = 'https://nexus.cloud.corporate.ge.com/nexus/content/repositories/' + settings['repoid'] + '/' + settings['groupid'] + '/' + settings['artifactid'] + '/maven-metadata.xml'
    def xml = new XmlSlurper().parseText(new URL(metadataUrl).getText())

    def versions = []
    xml.versioning.versions.children().each { versionXml ->
        versions << versionXml.text()
    }

    return versions
}

@NonCPS
def getPackageVersions(Map settings) {
    return getPackageVersionsList(settings).join('\n')
}

def copyPackageRepos(Map settings) {
    def requiredFields = ['groupid', 'artifactid', 'fileext', 'version', 'srcrepo', 'dstrepo']

    def missingFields = []
    requiredFields.each { field ->
        if (!settings.containsKey(field)) {
            missingFields.add(field)
        }
    }
    if (missingFields.size() > 0) {
        throw new java.security.InvalidParameterException("copyPackage() missing field(s): " + missingFields.join(', '))
    }

    sh """\
#!/bin/bash

set -ex

export JAVA_HOME=/appl/tools/java/jdk1.8.0_25

wget -v --content-disposition "https://nexus.cloud.corporate.ge.com/nexus/service/local/artifact/maven/content?g=${settings['groupid']}&a=${settings['artifactid']}&r=${settings['srcrepo']}&p=${settings['fileext']}&v=${settings['version']}"

/appl/tools/apache-maven-3.1.1/bin/mvn deploy:deploy-file \\
  -DgroupId=${settings['groupid']} \\
  -DartifactId=${settings['artifactid']} \\
  -Dversion=${settings['version']} \\
  -Dpackaging=${settings['fileext']} \\
  -DrepositoryId=nexus \\
  -DgeneratePom=true \\
  -Dfile=./${settings['artifactid']}-${settings['version']}.${settings['fileext']} \\
  -Durl=https://nexus.cloud.corporate.ge.com/nexus/content/repositories/${settings['dstrepo']}
"""
}

def deleteArtifact(Map settings) {
    def requiredFields = ['repoid', 'groupid', 'artifactid', 'version']
    
    def missingFields = []
    for (int i = 0; i < requiredFields.size(); i++) {
        def field = requiredFields[i]
        if (!settings.containsKey(field)) {
            missingFields.add(field)
        }
    }
    if (missingFields.size() > 0) {
        throw new java.security.InvalidParameterException("getPackageVersions() missing field(s): " + missingFields.join(', '))
    }

    def apiUrl = "https://nexus.cloud.corporate.ge.com/nexus/service/local/repositories/${settings.repoid}/content/${settings.groupid}/${settings.artifactid}/${settings.version}"
    
    httpRequest(authentication: 'nexus', timeout: 30, httpMode: 'DELETE', url: apiUrl, validResponseCodes: '204')
}

def artifactRollback(Map settings) {
    def requiredFields = ['repoid', 'groupid', 'artifactid', 'version']
    
    def missingFields = []
    
    for (int i = 0; i < requiredFields.size(); i++) {
        def field = requiredFields[i]
        if (!settings.containsKey(field)) {
            missingFields.add(field)
        }
    }
    if (missingFields.size() > 0) {
        throw new java.security.InvalidParameterException("getPackageVersions() missing field(s): " + missingFields.join(', '))
    }
    
    def versions = getPackageVersionsList(settings)
    
    for (int i = 0; i < versions.size(); i++) {
        def v = versions[i]
        if (isVersionNewer(v, settings.version)) {
            deleteArtifact(repoid: settings.repoid, groupid: settings.groupid, artifactid: settings.artifactid, version: v)
        }
    }
}

@NonCPS
def isVersionNewer(String versionA, String versionB) {
    List verA = versionA.tr('-', '.').tokenize('.')
    List verB = versionB.tr('-', '.').tokenize('.')

    def commonIndices = Math.min(verA.size(), verB.size())

    for (int i = 0; i < commonIndices; ++i) {
        def numA = verA[i].toInteger()
        def numB = verB[i].toInteger()

        if (numA != numB) {
            return numA > numB
        }
    }

    // If we got this far then all the common indices are identical, so whichever version is longer must be more recent
    return verA.size() > verB.size()
}
