package net.simlei.sbt.projectionist

import sbt._
import sbt.Keys._
import xsbti.compile.CompileAnalysis


object Projectionist {
  import org.json4s._
  import org.json4s.native.JsonMethods._
  implicit val formats: Formats = org.json4s.native.Serialization.formats(NoTypeHints)
  // implicit val formats2: Formats = org.json4s.native.Serialization.formats(Pretty)

  case class SubProject(
    projectRef: ProjectRef, 
    absolutePath: os.Path,
    relativePath: os.RelPath,
    compile_sourceDirectories: List[os.Path],
    test_sourceDirectories: List[os.Path],
  ){
    def projectId = projectRef.project
  }

  case class Entry(key: String, value: Map[String, String]) {
    def toTuple = key -> value
  }

  def generateJSON(buildStructure: sbt.internal.BuildStructure, baseDirectoryAbsolute: os.Path, subprojects: List[SubProject]): String = {
    // val rootProject = subprojects.filter{sub => sub.absolutePath != baseDirectoryAbsolute}.head
    def makeDispatchArg(subProject: SubProject, sourceDirectory: os.Path, keyPattern: String, sbtScope: String) = {
      val sbtTaskInvocation = s"${subProject.projectId} / $sbtScope / runMain {dirname|dot}.{basename}Main"
      s"-compiler=generic sbt-instance '$sbtTaskInvocation'"
    }
    def makeSourceDirectoryEntry(sub: SubProject, sbtScope: String, sourceDirectory: os.Path) = {
      val isJava = sourceDirectory.segments.toList.last == "java"
      val typeArg = sbtScope match {
        case "Test" => "test"
        case "Compile" =>
          isJava match {
            case true => "javasource"
            case false => "source"
          }
      }
      val filePattern = isJava match {
        case true => "*.java"
        case false => "*.scala"
      }
      val keyPattern = sourceDirectory.relativeTo(baseDirectoryAbsolute) + "/" + filePattern
      val dispatchArg = makeDispatchArg(sub, sourceDirectory, keyPattern, sbtScope)
      Entry( keyPattern, Map(
        "type" -> typeArg, 
        "dispatch" -> dispatchArg
      ))
    }

    val projectEntries = List(
        Entry("*.sbt"            , Map( "type" -> "build", "dispatch" -> "-compiler=generic sbt-instance reload" )),
        Entry("project/*.scala"  , Map( "type" -> "build", "dispatch" -> "-compiler=generic sbt-instance reload" )),
        Entry("*/project/*.scala", Map( "type" -> "build", "dispatch" -> "-compiler=generic sbt-instance reload" )),
      )
    def entriesForSubproject(sub: SubProject): List[Entry] = {
      sub.compile_sourceDirectories.map(makeSourceDirectoryEntry(sub, "Compile", _)) ++
      sub.test_sourceDirectories.map(makeSourceDirectoryEntry(sub, "Test", _))
    }
    val body = subprojects.flatMap{entriesForSubproject}
    // val body = (projectEntries ++ subprojects.flatMap{entriesForSubproject})
    val toplevel = (body.map{_.toTuple}).toMap
    org.json4s.native.Serialization.write(toplevel)
  }

}
