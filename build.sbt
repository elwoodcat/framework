import Dependencies._

organization in ThisBuild          := "net.liftweb"

version in ThisBuild               := "2.5-SNAPSHOT"

homepage in ThisBuild              := Some(url("http://www.liftweb.net"))

licenses in ThisBuild              += ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

startYear in ThisBuild             := Some(2006)

organizationName in ThisBuild      := "WorldWide Conferencing, LLC"

crossScalaVersions in ThisBuild    := Seq("2.9.1-1", "2.9.1", "2.9.0-1", "2.9.0", "2.8.2", "2.8.1", "2.8.0")

libraryDependencies in ThisBuild <++= scalaVersion { sv => Seq(specs, scalacheck).map(_(sv)) }

// Settings for Sonatype compliance
pomIncludeRepository in ThisBuild  := { _ => false }

publishTo in ThisBuild            <<= isSnapshot(if (_) Some(PublishRepo.Snapshot) else Some(PublishRepo.Staging))

// TODO: Enable after SBT 0.12.0-M2
//scmInfo in ThisBuild               := Some(ScmInfo(url("https://github.com/lift/framework"), "scm:git:https://github.com/lift/framework.git"))
//
//pomExtra in ThisBuild              ~= (_ ++ {Developers.toXml})

pomExtra in ThisBuild              ~= (_ ++ <scm>
                                              <url>http://github.com/lift/framework</url>
                                              <connection>scm:git:https://github.com/lift/framework.git</connection>
                                            </scm> ++ {Developers.toXml})

credentials in ThisBuild <+= state map { s => Credentials(BuildPaths.getGlobalSettingsDirectory(s, BuildPaths.getGlobalBase(s)) / ".credentials") }

initialize <<= (name, version) apply printLogo
