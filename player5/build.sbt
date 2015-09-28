name := "player"

version := "1.0"

scalaVersion := "2.11.6"

assemblySettings


libraryDependencies ++= Seq(
  "org.scalafx"            %% "scalafx"          % "8.0.31-R7",
  "org.scala-lang.modules" %% "scala-xml"        % "1.0.3",
  "com.typesafe.akka"      %% "akka-contrib"     % "2.3.4",
  "com.typesafe.akka"      %% "akka-cluster"     % "2.3.9",
  "com.typesafe.akka"      %% "akka-remote"      % "2.3.2",
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.3",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3",
  "commons-net"            % "commons-net"       % "2.0"

)

resolvers += Opts.resolver.sonatypeSnapshots


scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint")


unmanagedResourceDirectories in Compile <+= baseDirectory { _/"src/main/scala"}



shellPrompt := { state => System.getProperty("user.name") + ":" + Project.extract(state).currentRef.project + "> " }


fork := true

fork in Test := true
//
//lazy val commonSettings = Seq(
//  version := "0.1-SNAPSHOT",
//  organization := "org.echo",
//  scalaVersion := "2.11.6"
//)
//
//lazy val app = (project in file(".")).
//  settings(commonSettings: _*).
//  settings(
//    name := "player",
//    javaOptions += "-Dfile.encoding=UTF-8",
//    fork in run := true
//  )
