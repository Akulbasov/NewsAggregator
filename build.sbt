name := "naggregator"

organization := "semantive"

scalaVersion := "2.11.8"

logLevel := Level.Debug


val mainClassString = ("FlushTokafka","AggregateToKafka")



scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")


parallelExecution := false

updateOptions:=updateOptions.value.withCachedResolution(true)

version := "0.1.0"


resolvers += Resolver.mavenLocal
libraryDependencies ++= {
    Seq(
        "com.typesafe" % "config" % "1.3.1",

        "com.github.catalystcode" %% "streaming-rss-html" % "1.0.2",
        "org.apache.spark" %% "spark-mllib" % "2.3.0",
    "org.apache.spark" % "spark-core_2.11" % "2.3.0",
        "org.apache.spark" % "spark-sql-kafka-0-10_2.11" % "2.3.0",
        "org.apache.spark" % "spark-sql_2.11" % "2.3.0",

        "org.apache.spark" %% "spark-streaming" % "2.3.0",
        "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.0.0"
    )
}

//--------------------------------
//---- sbt-assembly settings for spark -----
//--------------------------------


mainClass in assembly := Some(mainClassString._1)

assemblyJarName := "run-app.jar"

assemblyMergeStrategy in assembly := {
    case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
    case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
    case "log4j.properties"                                  => MergeStrategy.discard
    case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
    case "reference.conf"                                    => MergeStrategy.concat
    case _                                                   => MergeStrategy.first
}

assemblyOption in assembly ~= { _.copy(cacheOutput = false) }

assemblyExcludedJars in assembly := {
    val cp = (fullClasspath in assembly).value
    cp filter { c =>
        c.data.getName.startsWith("log4j")
        c.data.getName.startsWith("slf4j-") ||
          c.data.getName.startsWith("scala-library")
    }
}

// Disable tests (they require Spark)
test in assembly := {}

// publish to artifacts directory
publishArtifact in(Compile, packageDoc) := false

publishTo := Some(Resolver.file("file", new File("artifacts")))

cleanFiles += baseDirectory { base => base / "artifacts" }.value




//--------------------------------
//----- sbt-docker settings ------
//--------------------------------


enablePlugins(sbtdocker.DockerPlugin)

dockerfile in docker := {
    val baseDir = baseDirectory.value
    val artifact: File = assembly.value

    val sparkHome = "/home/spark"
    val imageAppBaseDir = "/app"
    val artifactTargetPath = s"$imageAppBaseDir/${artifact.name}"

    val dockerResourcesDir = baseDir / "docker-resources"
    val dockerResourcesTargetPath = s"$imageAppBaseDir/"

    new Dockerfile {
        from("akulbasov1994/spark:1.0")
        maintainer("akulbasov")
        env("APP_BASE", s"$imageAppBaseDir")
        env("APP_CLASS", mainClassString._1)
        env("APP_CLASS_KAFKA", mainClassString._2)
        env("SPARK_HOME", sparkHome)
        copy(artifact, artifactTargetPath)
        copy(dockerResourcesDir, dockerResourcesTargetPath)
        //Symlink the service jar to a non version specific name
        run("chmod", "+x", s"${dockerResourcesTargetPath}/spark-entrypoint.sh")
//        run("hdfs dfs","-mkdir", "/tmp/sparkCheckpoint")
        entryPoint(s"${dockerResourcesTargetPath}/spark-entrypoint.sh")
    }
}
buildOptions in docker := BuildOptions(cache = true)

imageNames in docker := Seq(
    ImageName(
        namespace = Some(organization.value.toLowerCase),
        repository = name.value,
        // We parse the IMAGE_TAG env var which allows us to override the tag at build time
        tag = Some(sys.props.getOrElse("IMAGE_TAG", default = version.value))
    )
)