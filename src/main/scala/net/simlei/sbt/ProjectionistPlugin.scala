package net.simlei.sbt
import net.simlei.sbt.projectionist._

import sbt._
import sbt.Keys._
import xsbti.compile.CompileAnalysis
import sbt.plugins.JvmPlugin

object ProjectionistPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin

  object autoImport {
    // val projectlist_root = settingKey[Project]("refers to the aggregating project")
    // val projectlist_isAggregate = settingKey[Boolean]("whether this is an aggegating build")
    // val projectlist = taskKey[String]("lists the subprojects in a custom format")
    val projectionist = taskKey[String]("generates a projectionist.json")
    val projectlist_subprojects = taskKey[List[Projectionist.SubProject]]("list of projects with already resolved keys")
  }

  import autoImport._

  def flattenTasks[A](tasks: Seq[Def.Initialize[Task[A]]]): Def.Initialize[Task[List[A]]] =
    tasks.toList match {
      case Nil => Def.task { Nil }
      case x :: xs => Def.taskDyn {
        flattenTasks(xs) map (x.value :: _)
      }
    }
  override lazy val projectSettings = Seq(
    projectlist_subprojects := Def.taskDyn {
        flattenTasks {
          for (subproject <- buildStructure.value.allProjectRefs)
            yield Def.task { 
              val absPath = os.Path((subproject / baseDirectory).value.getAbsolutePath)
              val basePath = os.Path((ThisBuild / baseDirectory).value)
              Projectionist.SubProject(
                subproject, 
                absPath,
                absPath.relativeTo(basePath),
                (subproject / Compile / unmanagedSourceDirectories).value.map{os.Path(_)}.toList,
                (subproject / Test / unmanagedSourceDirectories).value.map{os.Path(_)}.toList,
              )
            }
        }
      }.value,
    projectionist := {
      val subprojects = projectlist_subprojects.value
      val baseDir = (ThisBuild / baseDirectory).value
      val json = Projectionist.generateJSON(buildStructure.value, os.Path(baseDir), subprojects)
      os.write.over(os.Path(baseDir / ".projections.json"), json)
      json
    },
  )

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq()
}
