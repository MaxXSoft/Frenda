organization := "net.maxxsoft"
name := "frenda"
version := "0.1"
scalaVersion := "2.13.7"

// FIRRTL
val firrtlVersion = "1.4.4"
lazy val firrtlLib = "edu.berkeley.cs" %% "firrtl" % firrtlVersion
libraryDependencies += firrtlLib

// Kryo
val kryoVersion = "5.2.0"
lazy val kryoLib = "com.esotericsoftware.kryo" % "kryo5" % kryoVersion
libraryDependencies += kryoLib
