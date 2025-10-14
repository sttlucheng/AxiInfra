package lmss.generator

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}
import lmss.LaomaSubsys

object Generator {
  val firtoolOpts = Seq(
    FirtoolOption("-O=release"),
    FirtoolOption("--disable-annotation-unknown"),
    FirtoolOption("--strip-debug-info"),
    //    FirtoolOption("--lower-memories"),
    FirtoolOption("--disable-all-randomization"),
    FirtoolOption("--add-vivado-ram-address-conflict-synthesis-bug-workaround"),
    FirtoolOption("--lowering-options=noAlwaysComb," +
      " disallowPortDeclSharing, disallowLocalVariables," +
      " emittedLineLength=120, explicitBitcast," +
      " locationInfoStyle=plain, disallowMuxInlining")
  )
}

object LmssGenerator extends App {
  val config = new LmssConfig
  (new ChiselStage).execute(args, Generator.firtoolOpts ++ Seq(
    ChiselGeneratorAnnotation(() => new LaomaSubsys()(config))
  ))
}