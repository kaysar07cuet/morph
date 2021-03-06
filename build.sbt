name := "morph-parent"

organization := "es.upm.fi.oeg.morph"

version := "1.0.4"

scalaVersion := "2.11.2"

crossPaths := false

libraryDependencies ++= Seq(
)

scalacOptions += "-deprecation"

EclipseKeys.skipParents in ThisBuild := false

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

unmanagedSourceDirectories in Compile <<= (scalaSource in Compile)(Seq(_))

unmanagedSourceDirectories in Test <<= (scalaSource in Test)(Seq(_))

publish := {}
