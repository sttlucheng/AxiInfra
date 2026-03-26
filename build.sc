import mill._
import scalalib._

// =============================================================================
// Version constants - aligned with xs-utils/build.mill (Chisel 7.9.0)
// =============================================================================
object v {
  val scala          = "2.13.18"
  val chiselVersion  = "7.9.0"
  val firtoolVersion = "1.140.0"

  val chiselPlugin   = ivy"org.chipsalliance:::chisel-plugin:$chiselVersion"
  val llvmFirtool    = ivy"org.chipsalliance:llvm-firtool:$firtoolVersion"
  val scalatest      = ivy"org.scalatest::scalatest:3.2.19"
  val scalacheck     = ivy"org.scalatestplus::scalacheck-1-18:3.2.19.0"
  val json4sj        = ivy"org.json4s::json4s-jackson:4.0.7"
  val mainargs       = ivy"com.lihaoyi::mainargs:0.5.0"
  val sourcecode     = ivy"com.lihaoyi::sourcecode:0.4.4"
  val osLib          = ivy"com.lihaoyi::os-lib:0.10.7"
  val upickle        = ivy"com.lihaoyi::upickle:3.3.1"
  val firtoolResolver = ivy"org.chipsalliance::firtool-resolver:2.0.1"
  val scopt          = ivy"com.github.scopt::scopt:4.1.0"
  val commonText     = ivy"org.apache.commons:commons-text:1.15.0"
  val json4s         = ivy"io.github.json4s::json4s-native:4.1.0"
  val dataclass      = ivy"io.github.alexarchambault::data-class:0.2.7"
  val scalaReflect   = ivy"org.scala-lang:scala-reflect:$scala"
  val unrollPlugin   = ivy"com.lihaoyi:::unroll-plugin:0.3.0"
  val unrollAnno     = ivy"com.lihaoyi::unroll-annotation:0.3.0"
}

val depBase = os.pwd / "xs-utils" / "dep"

// Common scalac options matching xs-utils/build.mill HasCommonOptions
val warnConf = Seq(
  "msg=APIs in chisel3.internal:s",
  "msg=All APIs in package firrtl:s",
  "msg=migration to the MLIR:s",
  "msg=method hasDefiniteSize in trait IterableOnceOps is deprecated:s",
  "msg=object JavaConverters in package collection is deprecated:s",
  "msg=undefined in comment for method cf in class PrintableHelper:s",
  "cat=deprecation&origin=firrtl\\.options\\.internal\\.WriteableCircuitAnnotation:s",
  "cat=deprecation&origin=firrtl\\.logger.*:s",
  "cat=deprecation&origin=chisel3\\.util\\.experimental\\.BoringUtils.*:s",
  "cat=deprecation&origin=chisel3\\.experimental\\.IntrinsicModule:s",
  "cat=deprecation&origin=chisel3\\.ltl.*:s",
  "cat=deprecation&origin=chisel3\\.InstanceId:s",
  "cat=deprecation&msg=Looking up Modules is deprecated:s",
  "cat=deprecation&msg=Use of @instantiable on user-defined types is deprecated:s",
  "cat=scala3-migration:s",
  "msg=reading InlineInfoAttribute from firtoolresolver:s",
  "msg=will no longer have a structural type:s"
).mkString(",")

val chiselCommonOptions = Agg(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Ymacro-annotations",
  "-language:reflectiveCalls",
  "-Xsource:3",
  s"-Wconf:$warnConf"
)

// =============================================================================
// Chisel sub-modules (split build matching xs-utils/build.mill structure)
// =============================================================================

// firrtl - IR definitions, no dependency on other chisel modules
object chiselFirrtl extends SbtModule {
  override def scalaVersion = v.scala
  override def millSourcePath = depBase / "chisel" / "firrtl"
  override def scalacOptions = super.scalacOptions() ++ chiselCommonOptions ++ Agg(
    "-language:existentials",
    "-language:implicitConversions",
    "-Xsource-features:infer-override"
  )
  override def ivyDeps = super.ivyDeps() ++ Agg(
    v.scopt, v.commonText, v.osLib, v.json4s, v.dataclass
  )
}

// macros - defines @instantiable, @public and other macro annotations
object chiselMacros extends SbtModule {
  override def scalaVersion = v.scala
  override def millSourcePath = depBase / "chisel" / "macros"
  override def sources = T.sources(
    millSourcePath / "src" / "main" / "scala-2"
  )
  override def scalacOptions = super.scalacOptions() ++ chiselCommonOptions
  override def ivyDeps = super.ivyDeps() ++ Agg(v.scalaReflect)
}

// core - main chisel3 core, depends on firrtl + macros
object chiselCore extends SbtModule {
  override def scalaVersion = v.scala
  override def millSourcePath = depBase / "chisel" / "core"
  override def sources = T.sources(
    millSourcePath / "src" / "main" / "scala",
    millSourcePath / "src" / "main" / "scala-2"
  )
  override def moduleDeps = super.moduleDeps ++ Seq(chiselFirrtl, chiselMacros)
  override def scalacOptions = super.scalacOptions() ++ chiselCommonOptions
  override def ivyDeps = super.ivyDeps() ++ Agg(v.osLib, v.upickle, v.firtoolResolver)

  def buildInfo = T {
    val outputFile = T.dest / "chisel3" / "BuildInfo.scala"
    val firtoolVersionString = "Some(\"" + v.firtoolVersion + "\")"
    val contents =
      s"""
         |package chisel3
         |case object BuildInfo {
         |  val buildInfoPackage: String = "chisel3"
         |  val version: String = "${v.chiselVersion}"
         |  val scalaVersion: String = "${v.scala}"
         |  val firtoolVersion: scala.Option[String] = $firtoolVersionString
         |  override val toString: String = {
         |    "buildInfoPackage: %s, version: %s, scalaVersion: %s, firtoolVersion %s".format(
         |        buildInfoPackage, version, scalaVersion, firtoolVersion
         |    )
         |  }
         |}
         |""".stripMargin
    os.write(outputFile, contents, createFolders = true)
    PathRef(T.dest)
  }

  override def generatedSources = T { super.generatedSources() :+ buildInfo() }
}

// svsim - SystemVerilog simulation interface
object chiselSvsim extends SbtModule {
  override def scalaVersion = v.scala
  override def millSourcePath = depBase / "chisel" / "svsim"
  override def scalacOptions = super.scalacOptions() ++ chiselCommonOptions
  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(v.unrollAnno)
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(v.unrollPlugin)
}

// chiselLib - top-level chisel library, re-exports everything
object chiselLib extends SbtModule {
  override def scalaVersion = v.scala
  override def millSourcePath = depBase / "chisel"
  override def sources = T.sources(
    millSourcePath / "src" / "main" / "scala",
    millSourcePath / "src" / "main" / "scala-2"
  )
  override def moduleDeps = super.moduleDeps ++ Seq(chiselCore, chiselSvsim)
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(v.chiselPlugin)
  override def scalacOptions = super.scalacOptions() ++ chiselCommonOptions ++ Agg(
    "-Ytasty-reader"
  )
  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(v.scalatest)
}

// =============================================================================
// Common trait for modules that depend on Chisel
// =============================================================================
trait ChiselModule extends ScalaModule {
  override def scalaVersion = v.scala
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(v.chiselPlugin)
  override def scalacOptions = super.scalacOptions() ++ Agg(
    "-language:reflectiveCalls",
    "-Ymacro-annotations",
    "-Ytasty-reader",
    "-Xsource:3",
    s"-Wconf:$warnConf"
  )
  override def ivyDeps = super.ivyDeps() ++ Agg(v.llvmFirtool)
  override def moduleDeps = super.moduleDeps ++ Seq(chiselLib)
}

// =============================================================================
// Infrastructure modules
// =============================================================================

object cde extends ChiselModule {
  override def millSourcePath = depBase / "cde" / "cde"
}

object diplomacy extends ChiselModule {
  override def millSourcePath = depBase / "diplomacy" / "diplomacy"
  override def moduleDeps = super.moduleDeps ++ Seq(cde)
  override def ivyDeps = super.ivyDeps() ++ Agg(v.sourcecode)
}

object rocketchip extends SbtModule with ChiselModule {
  override def millSourcePath = depBase / "rocket-chip"
  override def moduleDeps = super.moduleDeps ++ Seq(hardfloat, cde, diplomacy, macros)
  override def ivyDeps = super.ivyDeps() ++ Agg(v.json4sj, v.mainargs)

  object macros extends SbtModule with ChiselModule {
    override def ivyDeps = super.ivyDeps() ++ Agg(v.scalaReflect)
  }

  object hardfloat extends SbtModule with ChiselModule {
    override def millSourcePath = depBase / "hardfloat" / "hardfloat"
  }
}

// =============================================================================
// xs-utils
// =============================================================================
object xsutils extends SbtModule with ChiselModule {
  override def millSourcePath = os.pwd / "xs-utils"
  override def moduleDeps = super.moduleDeps ++ Seq(rocketchip, cde)
}

// =============================================================================
// Main project - LaomaSubsystem
// =============================================================================
object lmss extends SbtModule with ChiselModule {
  override def millSourcePath = os.pwd
  override def moduleDeps = super.moduleDeps ++ Seq(rocketchip, xsutils)

  object test extends SbtModuleTests with TestModule.ScalaTest {
    override def ivyDeps = super.ivyDeps() ++ Agg(v.scalatest)
    def testFramework = "org.scalatest.tools.Framework"
  }
}