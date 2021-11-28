organization := "net.maxxsoft"
name := "frenda"
version := "0.0.2"
scalaVersion := "2.13.7"

// FIRRTL
val firrtlVersion = "1.4.4"
lazy val firrtlLib = "edu.berkeley.cs" %% "firrtl" % firrtlVersion
libraryDependencies += firrtlLib

// Chill
val chillVersion = "0.10.0"
lazy val chillLib = "com.twitter" %% "chill" % chillVersion
libraryDependencies += chillLib

// Assembly settings
assembly / assemblyJarName := "frenda.jar"
assembly / test := {}
assembly / assemblyOutputPath := file("./utils/bin/frenda.jar")
