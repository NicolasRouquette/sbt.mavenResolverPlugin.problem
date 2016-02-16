import sbt.Keys._
import sbt._
import net.virtualvoid.sbt.graph._

resolvers += new MavenRepository("My Repo", "https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content")

lazy val root = Project("bundleTest", file("."))
  .enablePlugins(com.typesafe.sbt.packager.universal.UniversalPlugin)
  .settings(
     organization := "org.example",
     
     version := "1.0",

     scalaVersion := "2.11.7",
   
     libraryDependencies += 
       "org.example" % "library-bundle" % "1.0" % "compile" artifacts
       Artifact("library-bundle", "zip", "zip", Some("resource"), Seq(), None, Map())
  )
