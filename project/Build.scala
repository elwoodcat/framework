/*
 * Copyright 2012 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import Keys._
import net.liftweb.sbt.LiftBuildPlugin._
import Dependencies._


object BuildDef extends Build {

  lazy val liftProjects = core ++ web ++ persistence

  lazy val framework =
    liftProject("lift-framework", file("."))
      .aggregate(liftProjects: _*)
      .settings(aggregatedSetting(sources in(Compile, doc)),
                aggregatedSetting(dependencyClasspath in(Compile, doc)),
                publishArtifact := false)

  // Core Projects
  // -------------
  lazy val core: Seq[ProjectReference] =
    Seq(common, actor, json, json_scalaz, json_ext, util)

  lazy val common =
    coreProject("common")
      .settings(description := "Common Libraties and Utilities",
                libraryDependencies ++= Seq(slf4j_api, logback, slf4j_log4j12, commons_codec))

  lazy val actor =
    coreProject("actor")
        .dependsOn(common)
        .settings(description := "Simple Actor",
                  parallelExecution in Test := false)

  lazy val json =
    coreProject("json")
        .settings(description := "JSON Library",
                  parallelExecution in Test := false,
                  libraryDependencies <++= scalaVersion { sv => Seq(scalap(sv), paranamer) })

  lazy val json_scalaz =
    coreProject("json-scalaz")
        .dependsOn(json)
        .settings(description := "JSON Library based on Scalaz",
                  libraryDependencies <+= scalaVersion(scalaz))

  lazy val json_ext =
    coreProject("json-ext")
        .dependsOn(common, json)
        .settings(description := "Extentions to JSON Library",
                  libraryDependencies ++= Seq(commons_codec, joda_time))

  lazy val util =
    coreProject("util")
        .dependsOn(actor, json)
        .settings(description := "Utilities Library",
                  parallelExecution in Test := false,
                  libraryDependencies ++= Seq(joda_time, commons_codec, javamail, log4j, htmlparser))


  // Web Projects
  // ------------
  lazy val web: Seq[ProjectReference] =
    Seq(testkit, webkit, wizard)

  lazy val testkit =
    webProject("testkit")
        .dependsOn(util)
        .settings(description := "Testkit for Webkit Library",
                  libraryDependencies ++= Seq(commons_httpclient, servlet_api))

  lazy val webkit =
    webProject("webkit")
        .dependsOn(util, testkit % "provided")
        .settings(description := "Webkit Library",
                  parallelExecution in Test := false,
                  libraryDependencies <++= scalaVersion { sv =>
                    Seq(commons_fileupload, servlet_api, specs(sv).copy(configurations = Some("provided")), jetty6, jwebunit)
                  },
                  initialize in Test <<= (sourceDirectory in Test) { src =>
                    System.setProperty("net.liftweb.webapptest.src.test.webapp", (src / "webapp").absString)
                  })

  lazy val wizard =
    webProject("wizard")
        .dependsOn(webkit, db)
        .settings(description := "Wizard Library")


  // Persistence Projects
  // --------------------
  lazy val persistence: Seq[ProjectReference] =
    Seq(db, proto, jpa, mapper, record, couchdb, squeryl_record, mongodb, mongodb_record, ldap)

  lazy val db =
    persistenceProject("db")
        .dependsOn(util)
        .settings(libraryDependencies += mockito_all)

  lazy val proto =
    persistenceProject("proto")
        .dependsOn(webkit)

  lazy val jpa =
    persistenceProject("jpa")
        .dependsOn(webkit)
        .settings(libraryDependencies ++= Seq(scalajpa, persistence_api))

  lazy val mapper =
    persistenceProject("mapper")
        .dependsOn(db, proto)
        .settings(description := "Mapper Library",
                  parallelExecution in Test := false,
                  libraryDependencies ++= Seq(h2, derby),
                  initialize in Test <<= (crossTarget in Test) { ct =>
                    System.setProperty("derby.stream.error.file", (ct / "derby.log").absolutePath)
                  })

  lazy val record =
    persistenceProject("record")
        .dependsOn(proto, db)

  lazy val couchdb =
    persistenceProject("couchdb")
        .dependsOn(record)
        .settings(libraryDependencies += dispatch_http)

  lazy val squeryl_record =
    persistenceProject("squeryl-record")
        .dependsOn(record, db)
        .settings(libraryDependencies ++= Seq(h2, squeryl))

  lazy val mongodb =
    persistenceProject("mongodb")
        .dependsOn(json_ext)
        .settings(parallelExecution in Test := false,
                  libraryDependencies += mongo_driver,
                  initialize in Test <<= (resourceDirectory in Test) { rd =>
                    System.setProperty("java.util.logging.config.file", (rd / "logging.properties").absolutePath)
                  })

  lazy val mongodb_record =
    persistenceProject("mongodb-record")
        .dependsOn(record, mongodb)
        .settings(parallelExecution in Test := false)

  lazy val ldap =
    persistenceProject("ldap")
        .dependsOn(mapper)
        .settings(libraryDependencies += apacheds,
                  initialize in Test <<= (crossTarget in Test) { ct =>
                    System.setProperty("apacheds.working.dir", (ct / "apacheds").absolutePath)
                  })

  def coreProject = liftProject("core") _
  def webProject = liftProject("web") _
  def persistenceProject = liftProject("persistence") _

  /** Project definition helper that simplifies creation of `ProjectReference`.
    *
    * It is a convenience method to create a Lift `ProjectReference` module by having the boilerplate for most common
    * activities tucked in.
    *
    * @param base     the base path location of project module.
    * @param prefix   the prefix of project module.
    * @param module   the name of the project module. Typically, a project id is of the form lift-`module`.
    */
  def liftProject(base: String, prefix: String = "lift-")(module: String): Project =
    liftProject(id = if (module.startsWith(prefix)) module else prefix + module,
                base = file(base) / module.stripPrefix(prefix))

  def liftProject(id: String, base: File): Project =
    Project(id, base).settings(liftBuildSettings: _*)
}
