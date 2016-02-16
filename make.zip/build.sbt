import sbt.Keys._
import sbt._
import net.virtualvoid.sbt.graph._

val resourceArtifact = settingKey[Artifact]("Specifies the project's resource artifact")

publishTo := Some(new MavenRepository("My Repo", "https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/staging/deploy/maven2"))

lazy val root = Project("library-bundle", file("."))
  .enablePlugins(com.typesafe.sbt.packager.universal.UniversalPlugin)
   .settings(
     projectID := {
       val previous = projectID.value
       previous.extra(
         "artifact.kind" -> "third_party.aggregate.libraries")
     }
   )
   .settings(

     organization := "org.example",
     
     version := "1.0",

     scalaVersion := "2.11.7",

     libraryDependencies ++= Seq(
       "com.assembla.scala-incubator" %% "graph-constrained" % "1.10.0" withSources() withJavadoc()
     ),

     crossScalaVersions := Seq(),

     // disable using the Scala version in output paths and artifacts
     crossPaths := false,

     // disable publishing the main jar produced by `package`
     publishArtifact in(Compile, packageBin) := false,

     // disable publishing the main API jar
     publishArtifact in(Compile, packageDoc) := false,

     // disable publishing the main sources jar
     publishArtifact in(Compile, packageSrc) := false,

     // disable publishing the jar produced by `test:package`
     publishArtifact in(Test, packageBin) := false,

     // disable publishing the test API jar
     publishArtifact in(Test, packageDoc) := false,

     // disable publishing the test sources jar
     publishArtifact in(Test, packageSrc) := false,

     resourceArtifact := 
       Artifact((name in Universal).value, "zip", "zip", Some("resource"), Seq(), None, Map()),

     artifacts += resourceArtifact.value,

     // contents of the '*-resource.zip' to be produced by 'universal:packageBin'
     mappings in Universal <++= (
       appConfiguration,
       classpathTypes,
       update,
       streams) map {
       (appC, cpT, up, s) =>

         val libDir = "aspectj/lib/"
         val srcDir = "aspectj/lib.sources/"
         val docDir = "aspectj/lib.javadoc/"

         val compileConfig: ConfigurationReport = {
           up.configurations.find((c: ConfigurationReport) => Configurations.Compile.name == c.configuration).get
         }

         def transitiveScope(modules: Set[Module], g: ModuleGraph): Set[Module] = {

           @annotation.tailrec
           def acc(focus: Set[Module], result: Set[Module]): Set[Module] = {
             val next = g.edges.flatMap { case (fID, tID) =>
               focus.find(m => m.id == fID).flatMap { _ =>
                 g.nodes.find(m => m.id == tID)
               }
             }.to[Set]
             if (next.isEmpty)
               result
             else
               acc(next, result ++ next)
           }

           acc(modules, Set())
         }

         val zipFiles: Set[File] = {
           val jars = for {
             oReport <- compileConfig.details
             mReport <- oReport.modules
             (artifact, file) <- mReport.artifacts
             if "zip" == artifact.extension
             file <- {
               s.log.info(s"compile: ${oReport.organization}, ${file.name}")
               s.log.info(s"extra deps: ${mReport.module.extraDependencyAttributes}")
               s.log.info(s"extra prop: ${mReport.module.extraAttributes}")
               val graph = backend.SbtUpdateReport.fromConfigurationReport(compileConfig, mReport.module)
               s.log.info(graph.nodes.mkString("graph nodes:\n","\n","\n"))
               s.log.info(graph.edges.mkString("graph edges:\n","\n","\n"))
               val roots: Set[Module] = graph.nodes.filter { m =>
                 m.id.organisation == mReport.module.organization &&
                   m.id.name == mReport.module.name &&
                   m.id.version == mReport.module.revision
               }.to[Set]
               s.log.info(s"roots: ${roots.mkString(",")}")
               val scope: Seq[Module] =
                 transitiveScope(roots, graph).to[Seq].sortBy( m => m.id.organisation + m.id.name)
               s.log.info(s"scope: ${scope.mkString(",")}")
               val files = scope.flatMap { m: Module => m.jarFile }.to[Seq].sorted
               s.log.info(s"Excluding ${files.size} jars from zip aggregate resource dependencies")
               files.foreach { f =>
                 s.log.info(s" exclude: ${f.getParentFile.getParentFile.name}/${f.getParentFile.name}/${f.name}")
               }
               files
             }
           } yield file
           jars.to[Set]
         }

         val fileArtifacts = for {
           oReport <- compileConfig.details
           organizationArtifactKey = s"{oReport.organization},${oReport.name}"
           mReport <- oReport.modules
           (artifact, file) <- mReport.artifacts
           if "jar" == artifact.extension && !zipFiles.contains(file)
         } yield (oReport.organization, oReport.name, file, artifact)

         val fileArtifactsByType = fileArtifacts.groupBy { case (_, _, _, a) =>
           a.`classifier`.getOrElse(a.`type`)
         }
         val jarArtifacts = fileArtifactsByType("jar")
         val srcArtifacts = fileArtifactsByType("sources")
         val docArtifacts = fileArtifactsByType("javadoc")

         val jars = jarArtifacts.map { case (o, _, jar, _) =>
           s.log.info(s"* jar: $o/${jar.name}")
           jar -> (libDir + jar.name)
         }
         val srcs = srcArtifacts.map { case (o, _, jar, _) =>
           s.log.info(s"* src: $o/${jar.name}")
           jar -> (srcDir + jar.name)
         }
         val docs = docArtifacts.map { case (o, _, jar, _) =>
           s.log.info(s"* doc: $o/${jar.name}")
           jar -> (docDir + jar.name)
         }

         jars ++ srcs ++ docs
     },

     artifacts <+= (name in Universal) { n => Artifact(n, "zip", "zip", Some("resource"), Seq(), None, Map()) },
     packagedArtifacts <+= (packageBin in Universal, name in Universal) map { (p, n) =>
       Artifact(n, "zip", "zip", Some("resource"), Seq(), None, Map()) -> p
     }
   )

