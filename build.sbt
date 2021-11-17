organization := "net.maxxsoft"
name := "frenda"
version := "0.1"
scalaVersion := "2.13.7"

// FIRRTL
val firrtlVersion = "1.4.4"
lazy val firrtlLib = "edu.berkeley.cs" %% "firrtl" % firrtlVersion
libraryDependencies += firrtlLib
