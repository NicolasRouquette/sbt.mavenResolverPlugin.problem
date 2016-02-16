# sbt.mavenResolverPlugin.problem

In principle, the [Maven Resolver Plugin](http://www.scala-sbt.org/0.13/docs/sbt-0.13-Tech-Previews.html#Maven+resolver+plugin) is great because it allows using Maven POM extra properties like this:

       libraryDependencies += 
         "org.example" % "library-bundle" % "1.0" % "compile" 
         extra("artifact.kind" -> "third_party.aggregate.libraries")
         artifacts Artifact("library-bundle", "zip", "zip", Some("resource"), Seq(), None, Map())
         
Without it, this does not work.

However, there is a subtle problem that affects the calculation of transitive dependencies.
Somehow, SBT is looking for `maven-metadata.xml` in a URL that includes the version number.

# With Maven Resolver Plugin

1) clean `~/.ivy2`

2) Change the remote Maven repository URL to publish to & resolve from:

  - [make.zip/build.sbt#L7](https://github.com/NicolasRouquette/sbt.mavenResolverPlugin.problem/blob/master/make.zip/build.sbt#L7)
  - [use.zip/build.sbt#L5](https://github.com/NicolasRouquette/sbt.mavenResolverPlugin.problem/blob/master/use.zip/build.sbt#L5)

3) Run sbt in `make.zip`:

  ```
  [1067] $ sbt
  [info] Loading global plugins from /home/rouquett/.sbt/0.13/plugins
  [info] Loading project definition from /opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/make.zip/project
  [info] Set current project to library-bundle (in build file:/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/make.zip/)
  > update
  [info] Updating {file:/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/make.zip/}library-bundle...
  [info] Resolving jline#jline;2.12.1 ...
  [info] Done updating.
  [success] Total time: 3 s, completed Feb 16, 2016 4:06:15 PM
  ```
  
  Did the update really work?
  
  ```
  > last *:update
  [info] Updating {file:/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/make.zip/}library-bundle...
  [debug] :: resolving dependencies :: org.example#library-bundle;1.0
  [debug] 	confs: [compile, runtime, test, provided, optional, compile-internal, runtime-internal, test-internal, plugin, sources, docs, pom, scala-tool]
  [debug] 	validate = true
  [debug] 	refresh = false
  [debug] resolving dependencies for configuration 'compile'
  [debug] == resolving dependencies for org.example#library-bundle;1.0 [compile]
  [debug] == resolving dependencies org.example#library-bundle;1.0->org.scala-lang#scala-library;2.11.7 [compile->default(compile)]
  [info] Resolving org.scala-lang#scala-library;2.11.7 ...
  [debug] sbt-chain: Checking cache for: sbt.ivyint.MergedDescriptors@4fd49d82
  [info] Resolving org.scala-lang#scala-library;2.11.7 ...
  [debug] sbt-chain-delegate: Checking cache for: sbt.ivyint.MergedDescriptors@4fd49d82
  [debug] 		tried /home/rouquett/.ivy2/local/org.scala-lang/scala-library/2.11.7/ivys/ivy.xml
  [debug] 	local: no ivy file found for org.scala-lang#scala-library;2.11.7
  [debug] CLIENT ERROR: Not Found url=https://jcenter.bintray.com/org/scala-lang/scala-library/2.11.7/maven-metadata.xml
  [debug] 	found org.scala-lang#scala-library;2.11.7 in jcenter
  ...
  ```
  
  SHA1 checksums do not match.
  `maven-metadata.xml` are looked up in the wrong place.
  E.g., instead of:
  
  https://jcenter.bintray.com/org/scala-lang/scala-library/2.11.7/maven-metadata.xml (does not exist)
  
  it should be:
  
  https://jcenter.bintray.com/org/scala-lang/scala-library/maven-metadata.xml (exists)
  
  
  Still, we can publish:
  
  > publish
  [info] * jar: com.assembla.scala-incubator/graph-constrained_2.11-1.10.0.jar
  [info] * src: com.assembla.scala-incubator/graph-constrained_2.11-1.10.0-sources.jar
  [info] * doc: com.assembla.scala-incubator/graph-constrained_2.11-1.10.0-javadoc.jar
  [info] Wrote /opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/make.zip/target/library-bundle-1.0.pom
  [info] :: delivering :: org.example#library-bundle;1.0 :: 1.0 :: release :: Tue Feb 16 16:07:53 UTC 2016
  [info] 	delivering ivy file to /opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/make.zip/target/ivy-1.0.xml
  [success] Total time: 4 s, completed Feb 16, 2016 4:07:56 PM
  ```
  
  On the Maven repo, we get several files:
  
  /org/example/library-bundle/1.0/library-bundle-1.0.pom
  /org/example/library-bundle/1.0/library-bundle-1.0-resource.zip
  /org/example/library-bundle/maven-metadata.xml
  
4) Run sbt in `use.zip`:

  ```
  [1011] $ sbt
  [info] Loading global plugins from /home/rouquett/.sbt/0.13/plugins
  [info] Loading project definition from /opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/project
  [info] Set current project to bundleTest (in build file:/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/)
  > update
  [info] Updating {file:/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/}bundleTest...
  [info] Resolving org.example#library-bundle;1.0 ...
  [info] Failed to read descriptor dependency: org.example#library-bundle;1.0 {compile=[default(compile)]} from jcenter, Failed to read artifact descriptor for org.example:library-bundle:jar:1.0
  [info] Failed to read descriptor dependency: org.example#library-bundle;1.0 {compile=[default(compile)]} from public, Failed to read artifact descriptor for org.example:library-bundle:jar:1.0
  [info] Resolving jline#jline;2.12.1 ...
  [info] Done updating.
  [success] Total time: 4 s, completed Feb 16, 2016 4:08:55 PM
  ```
  
  Did it really work?
  
  ```
  > last *:update
  [info] Updating {file:/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/}bundleTest...
  [debug] :: resolving dependencies :: org.example#bundletest_2.11;1.0
  [debug] 	confs: [compile, runtime, test, provided, optional, compile-internal, runtime-internal, test-internal, plugin, sources, docs, pom, scala-tool]
  [debug] 	validate = true
  [debug] 	refresh = false
  [debug] resolving dependencies for configuration 'compile'
  [debug] == resolving dependencies for org.example#bundletest_2.11;1.0 [compile]
  [debug] == resolving dependencies org.example#bundletest_2.11;1.0->org.scala-lang#scala-library;2.11.7 [compile->default(compile)]
  [info] Resolving org.scala-lang#scala-library;2.11.7 ...
  [debug] sbt-chain: Checking cache for: sbt.ivyint.MergedDescriptors@78f0b737
  [info] Resolving org.scala-lang#scala-library;2.11.7 ...
  [debug] sbt-chain-delegate: Checking cache for: sbt.ivyint.MergedDescriptors@78f0b737
  [debug] 		tried /home/rouquett/.ivy2/local/org.scala-lang/scala-library/2.11.7/ivys/ivy.xml
  [debug] 	local: no ivy file found for org.scala-lang#scala-library;2.11.7
  [debug] CLIENT ERROR: Not Found url=https://jcenter.bintray.com/org/scala-lang/scala-library/2.11.7/maven-metadata.xml
  [debug] 	found org.scala-lang#scala-library;2.11.7 in jcenter
  [debug] == resolving dependencies org.example#bundletest_2.11;1.0->org.scala-lang#scala-library;2.11.7 [compile->runtime]
  [debug] == resolving dependencies org.example#bundletest_2.11;1.0->org.scala-lang#scala-library;2.11.7 [compile->compile]
  [debug] == resolving dependencies org.example#bundletest_2.11;1.0->org.scala-lang#scala-library;2.11.7 [compile->master]
  [debug] == resolving dependencies org.example#bundletest_2.11;1.0->org.example#library-bundle;1.0 [compile->default(compile)]
  [info] Resolving org.example#library-bundle;1.0 ...
  [debug] sbt-chain: Checking cache for: dependency: org.example#library-bundle;1.0 {compile=[default(compile)]}
  [info] Resolving org.example#library-bundle;1.0 ...
  [debug] sbt-chain-delegate: Checking cache for: dependency: org.example#library-bundle;1.0 {compile=[default(compile)]}
  [debug] 		tried /home/rouquett/.ivy2/local/org.example/library-bundle/1.0/ivys/ivy.xml
  [debug] 	local: no ivy file found for org.example#library-bundle;1.0
  [debug] CLIENT ERROR: Not Found url=https://jcenter.bintray.com/org/example/library-bundle/1.0/library-bundle-1.0.pom
  [info] Failed to read descriptor dependency: org.example#library-bundle;1.0 {compile=[default(compile)]} from jcenter, Failed to read artifact descriptor for org.example:library-bundle:jar:1.0
  [debug] CLIENT ERROR: Not Found url=https://repo1.maven.org/maven2/org/example/library-bundle/1.0/library-bundle-1.0.pom
  [info] Failed to read descriptor dependency: org.example#library-bundle;1.0 {compile=[default(compile)]} from public, Failed to read artifact descriptor for org.example:library-bundle:jar:1.0
  [debug] CLIENT ERROR: Not Found url=https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/maven-metadata.xml
  [debug] 	found org.example#library-bundle;1.0 in My Repo
  ```
  
  No, same problems as before.
 
# Without Maven Resolver Plugin

1) Delete [make.zip/project/maven.sbt](https://github.com/NicolasRouquette/sbt.mavenResolverPlugin.problem/blob/master/make.zip/project/maven.sbt)

2) Run sbt in `make.zip`:

  ```
  > sbt
  ...
  > update
  ...
  > last *:update
  ```
   
  Note that there are no "CLIENT ERROR: Not found url=....<version>/maven-metadata.xml"
   
  ```
  > publish
  [info] * jar: com.assembla.scala-incubator/graph-constrained_2.11-1.10.0.jar
  [info] * jar: com.assembla.scala-incubator/graph-core_2.11-1.10.0.jar
  [info] * jar: org.scalacheck/scalacheck_2.11-1.12.5.jar
  [info] * jar: org.scala-sbt/test-interface-1.0.jar
  [info] * jar: org.scala-lang/scala-library-2.11.7.jar
  [info] * src: com.assembla.scala-incubator/graph-constrained_2.11-1.10.0-sources.jar
  [info] * doc: com.assembla.scala-incubator/graph-constrained_2.11-1.10.0-javadoc.jar
  [info] Wrote /opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/make.zip/target/library-bundle-1.0.pom
  [info] :: delivering :: org.example#library-bundle;1.0 :: 1.0 :: release :: Tue Feb 16 16:20:16 UTC 2016
  [info] 	delivering ivy file to /opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/make.zip/target/ivy-1.0.xml
  [info] 	published library-bundle to https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/staging/deploy/maven2/org/example/library-bundle/1.0/library-bundle-1.0.pom
  [info] 	published library-bundle to https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/staging/deploy/maven2/org/example/library-bundle/1.0/library-bundle-1.0-resource.zip
  ```
  
  Notice that now the library-bundle contains the transitive closure of all the dependencies (at least the jars, but not the sources or javadoc)
  
3) Delete [use.zip/project/maven.sbt](https://github.com/NicolasRouquette/sbt.mavenResolverPlugin.problem/blob/master/use.zip/project/maven.sbt)

4) Run sbt in `use.zip`:

  ```
  [1014] $ sbt
  [info] Loading global plugins from /home/rouquett/.sbt/0.13/plugins
  [info] Loading project definition from /opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/project
  [info] Set current project to bundleTest (in build file:/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/)
  > update
  [info] Updating {file:/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/}bundleTest...
  [info] Resolving org.example#library-bundle;1.0 ...
  [error] 	My Repo: bad artifact.kind found in https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0.pom: expected='third_party.aggregate.libraries' found='null'
  [info] Resolving jline#jline;2.12.1 ...
  [warn] 	::::::::::::::::::::::::::::::::::::::::::::::
  [warn] 	::          UNRESOLVED DEPENDENCIES         ::
  [warn] 	::::::::::::::::::::::::::::::::::::::::::::::
  [warn] 	:: org.example#library-bundle;1.0: java.text.ParseException: inconsistent module descriptor file found in 'https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0.pom': bad artifact.kind found in https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0.pom: expected='third_party.aggregate.libraries' found='null';
  [warn] 	::::::::::::::::::::::::::::::::::::::::::::::
  [warn] 
  [warn] 	Note: Some unresolved dependencies have extra attributes.  Check that these dependencies exist with the requested attributes.
  [warn] 		org.example:library-bundle:1.0 (artifact.kind=third_party.aggregate.libraries)
  [warn] 
  [warn] 	Note: Unresolved dependencies path:
  [warn] 		org.example:library-bundle:1.0 (artifact.kind=third_party.aggregate.libraries) (/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/build.sbt#L16)
  [warn] 		  +- org.example:bundletest_2.11:1.0
  [trace] Stack trace suppressed: run last *:update for the full output.
  [error] (*:update) sbt.ResolveException: unresolved dependency: org.example#library-bundle;1.0: java.text.ParseException: inconsistent module descriptor file found in 'https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0.pom': bad artifact.kind found in https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0.pom: expected='third_party.aggregate.libraries' found='null';
  [error] Total time: 5 s, completed Feb 16, 2016 4:26:38 PM
  > reload
  [info] Loading global plugins from /home/rouquett/.sbt/0.13/plugins
  [info] Loading project definition from /opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/project
  [warn] Discarding 1 session setting.  Use 'session save' to persist session settings.
  [info] Set current project to bundleTest (in build file:/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/)
  > update
  [info] Updating {file:/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/}bundleTest...
  [info] Resolving jline#jline;2.12.1 ...
  [info] downloading https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0-resource.zip ...
  [info] 	[SUCCESSFUL ] org.example#library-bundle;1.0!library-bundle.zip (2788ms)
  [info] Done updating.
  [success] Total time: 5 s, completed Feb 16, 2016 4:27:19 PM
  > last *:update
  [info] Updating {file:/opt/local/imce/users/nfr/github.nfr/sbt.mavenResolverPlugin.problem/use.zip/}bundleTest...
  [debug] :: resolving dependencies :: org.example#bundletest_2.11;1.0
  [debug] 	confs: [compile, runtime, test, provided, optional, compile-internal, runtime-internal, test-internal, plugin, sources, docs, pom, scala-tool]
  [debug] 	validate = true
  [debug] 	refresh = false
  [debug] resolving dependencies for configuration 'compile'
  [debug] == resolving dependencies for org.example#bundletest_2.11;1.0 [compile]
  [debug] == resolving dependencies org.example#bundletest_2.11;1.0->org.scala-lang#scala-library;2.11.7 [compile->default(compile)]
  [info] Resolving org.scala-lang#scala-library;2.11.7 ...
  [debug] sbt-chain: Checking cache for: sbt.ivyint.MergedDescriptors@34065e98
  [debug] sbt-chain: module revision found in cache: org.scala-lang#scala-library;2.11.7
  [debug] 	found org.scala-lang#scala-library;2.11.7 in sbt-chain
  [debug] 	[2.11.7] org.scala-lang#scala-library;2.11.7
  [debug] == resolving dependencies org.example#bundletest_2.11;1.0->org.scala-lang#scala-library;2.11.7 [compile->runtime]
  [debug] == resolving dependencies org.example#bundletest_2.11;1.0->org.scala-lang#scala-library;2.11.7 [compile->compile]
  [debug] == resolving dependencies org.example#bundletest_2.11;1.0->org.scala-lang#scala-library;2.11.7 [compile->master]
  [debug] == resolving dependencies org.example#bundletest_2.11;1.0->org.example#library-bundle;1.0 [compile->default(compile)]
  [info] Resolving org.example#library-bundle;1.0 ...
  [debug] sbt-chain: Checking cache for: dependency: org.example#library-bundle;1.0 {compile=[default(compile)]}
  [info] Resolving org.example#library-bundle;1.0 ...
  [debug] sbt-chain-delegate: Checking cache for: dependency: org.example#library-bundle;1.0 {compile=[default(compile)]}
  [debug] 		tried /home/rouquett/.ivy2/local/org.example/library-bundle/1.0/ivys/ivy.xml
  [debug] 	local: no ivy file found for org.example#library-bundle;1.0
  [debug] 		tried https://jcenter.bintray.com/org/example/library-bundle/1.0/library-bundle-1.0.pom
  [debug] CLIENT ERROR: Not Found url=https://jcenter.bintray.com/org/example/library-bundle/1.0/library-bundle-1.0.pom
  [debug] 	jcenter: no ivy file found for org.example#library-bundle;1.0
  [debug] 		tried https://repo1.maven.org/maven2/org/example/library-bundle/1.0/library-bundle-1.0.pom
  [debug] CLIENT ERROR: Not Found url=https://repo1.maven.org/maven2/org/example/library-bundle/1.0/library-bundle-1.0.pom
  [debug] 	public: no ivy file found for org.example#library-bundle;1.0
  [debug] 		tried https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0.pom
  [debug] 	My Repo: found md file for org.example#library-bundle;1.0
  [debug] 		=> https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0.pom (1.0)
  [debug] downloading https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0.pom ...
  [debug] 	My Repo: downloading https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0.pom
  [debug] 	My Repo: downloading https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0.pom.sha1
  [debug] sha1 OK for https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/repo_groups/jpl.beta.group/content/org/example/library-bundle/1.0/library-bundle-1.0.pom
  ```
  
  Everything is resolved properly and the SHA1 checksums match!
  
