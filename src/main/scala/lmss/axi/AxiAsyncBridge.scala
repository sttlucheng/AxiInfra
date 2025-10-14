package lmss.axi

import chisel3._
import chisel3.experimental.noPrefix
import chisel3.util._
import freechips.rocketchip.util.{AsyncBundle, AsyncQueueParams, AsyncQueueSink, AsyncQueueSource}

class AxiAsyncSource(axiP:AxiParams, asyncP:AsyncQueueParams) extends Module {
  val s_axi = IO(Flipped(new AxiBundle(axiP)))
  val async = IO(new AsyncAxiBundle(axiP, asyncP))

  private val rx = Seq("aw", "ar", "w")
  private val tx = Seq("r", "b")

  for(chn <- rx) noPrefix {
    val sp = s_axi.elements(chn).asInstanceOf[DecoupledIO[Data]]
    val ap = async.elements(chn).asInstanceOf[AsyncBundle[Data]]
    val src = Module(new AsyncQueueSource(UInt(sp.bits.getWidth.W), asyncP))
    src.io.enq.valid := sp.valid
    src.io.enq.bits := sp.bits.asTypeOf(src.io.enq.bits)
    sp.ready := src.io.enq.ready
    ap <> src.io.async
    src.suggestName(s"src_$chn")
  }
  for(chn <- tx) noPrefix {
    val sp = s_axi.elements(chn).asInstanceOf[DecoupledIO[Data]]
    val ap = async.elements(chn).asInstanceOf[AsyncBundle[Data]]
    val sink = Module(new AsyncQueueSink(UInt(sp.bits.getWidth.W), asyncP))
    sink.io.async <> ap
    sp.valid := sink.io.deq.valid
    sp.bits := sink.io.deq.bits.asTypeOf(sp.bits)
    sink.io.deq.ready := sp.ready
    sink.suggestName(s"sink_$chn")
  }
}

class AxiAsyncSink(axiP:AxiParams, asyncP:AsyncQueueParams) extends Module {
  val async = IO(Flipped(new AsyncAxiBundle(axiP, asyncP)))
  val m_axi = IO(new AxiBundle(axiP))

  private val rx = Seq("r", "b")
  private val tx = Seq("aw", "ar", "w")

  for(chn <- rx) noPrefix {
    val sp = m_axi.elements(chn).asInstanceOf[DecoupledIO[Data]]
    val ap = async.elements(chn).asInstanceOf[AsyncBundle[Data]]
    val src = Module(new AsyncQueueSource(UInt(sp.bits.getWidth.W), asyncP))
    src.io.enq.valid := sp.valid
    src.io.enq.bits := sp.bits.asTypeOf(src.io.enq.bits)
    sp.ready := src.io.enq.ready
    ap <> src.io.async
    src.suggestName(s"src_$chn")
  }
  for(chn <- tx) noPrefix {
    val sp = m_axi.elements(chn).asInstanceOf[DecoupledIO[Data]]
    val ap = async.elements(chn).asInstanceOf[AsyncBundle[Data]]
    val sink = Module(new AsyncQueueSink(UInt(sp.bits.getWidth.W), asyncP))
    sink.io.async <> ap
    sp.valid := sink.io.deq.valid
    sp.bits := sink.io.deq.bits.asTypeOf(sp.bits)
    sink.io.deq.ready := sp.ready
    sink.suggestName(s"sink_$chn")
  }
}
