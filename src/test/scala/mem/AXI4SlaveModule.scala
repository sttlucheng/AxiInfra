// SPDX-License-Identifier: MulanPSL-2.0
// Copyright (c) 2025-2026 RedRISC Technology Co. Ltd.

package mem

import chisel3._
import chisel3.util._
import freechips.rocketchip.amba.axi4.{AXI4Parameters, AXI4SlaveNode, AXI4SlaveParameters, AXI4SlavePortParameters}
import freechips.rocketchip.diplomacy.{AddressSet, RegionType, TransferSizes}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule._
import xs.utils.HoldUnless

abstract class AXI4SlaveModule[T <: Data]
(
  address: Seq[AddressSet],
  executable: Boolean = true,
  beatBytes: Int = 8,
  burstLen: Int = 1,
  val _extra: T = null
)(implicit p: Parameters) extends LazyModule {

  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    Seq(AXI4SlaveParameters(
      address,
      regionType = RegionType.UNCACHED,
      executable = executable,
      supportsWrite = TransferSizes(1, beatBytes * burstLen),
      supportsRead = TransferSizes(1, beatBytes * burstLen),
      interleavedId = Some(0)
    )),
    beatBytes = beatBytes
  )))

  lazy val module = new AXI4SlaveModuleImp[T](this)

}

object MaskExpand {
  def apply(m: UInt, maskWidth: Int = 8): UInt = Cat(m.asBools.map(Fill(maskWidth, _)).reverse)
  def apply(m: Seq[Bool], maskWidth: Int): Vec[UInt] = VecInit(m.map(Fill(maskWidth, _)))
}

class AXI4SlaveModuleImp[T<:Data](outer: AXI4SlaveModule[T])
  extends LazyModuleImp(outer)
{
  val io = IO(new Bundle {
    val extra = if(outer._extra == null) None else Some(outer._extra.cloneType)
    val init_done = Output(Bool())
  })

  io.init_done := true.B

  val (in, edge) = outer.node.in.head
  // do not let MMIO AXI signals optimized out
  chisel3.dontTouch(in)


  when(in.aw.fire){
    assert(in.aw.bits.burst === AXI4Parameters.BURST_INCR, "only support busrt incr!")
  }
  when(in.ar.fire){
    assert(in.ar.bits.burst === AXI4Parameters.BURST_INCR, "only support busrt incr!")
  }

  val s_idle :: s_rdata :: s_wdata :: s_wresp :: Nil = Enum(4)

  val state = RegInit(s_idle)

  switch(state){
    is(s_idle){
      when(in.ar.fire){
        state := s_rdata
      }
      when(in.aw.fire){
        state := s_wdata
      }
    }
    is(s_rdata){
      when(in.r.fire && in.r.bits.last){
        state := s_idle
      }
    }
    is(s_wdata){
      when(in.w.fire && in.w.bits.last){
        state := s_wresp
      }
    }
    is(s_wresp){
      when(in.b.fire){
        state := s_idle
      }
    }
  }


  val fullMask = MaskExpand(in.w.bits.strb)

  def genWdata(originData: UInt) = (originData & (~fullMask).asUInt) | (in.w.bits.data & fullMask)

  val raddr = Wire(UInt())
  val (readBeatCnt, rLast) = {
    val c = Counter(256)
    val len = HoldUnless(in.ar.bits.len, in.ar.fire)
    raddr := HoldUnless(in.ar.bits.addr, in.ar.fire)
    in.r.bits.last := (c.value === len)

    when(in.r.fire) {
      c.inc()
      when(in.r.bits.last) {
        c.value := 0.U
      }
    }
    when(in.ar.fire) {
      assert(
        in.ar.bits.len === 0.U ||
          in.ar.bits.len === 1.U ||
          in.ar.bits.len === 3.U ||
          in.ar.bits.len === 7.U ||
          in.ar.bits.len === 15.U
      )
    }
    (c.value, in.r.bits.last)
  }

  in.ar.ready := state === s_idle
  in.r.bits.resp := AXI4Parameters.RESP_OKAY
  in.r.valid := state === s_rdata


  val waddr = Wire(UInt())
  val (writeBeatCnt, wLast) = {
    val c = Counter(256)
    waddr := HoldUnless(in.aw.bits.addr, in.aw.fire)
    when(in.w.fire) {
      c.inc()
      when(in.w.bits.last) {
        c.value := 0.U
      }
    }
    (c.value, in.w.bits.last)
  }

  in.aw.ready := state === s_idle && !in.ar.valid
  in.w.ready := state === s_wdata

  in.b.bits.resp := AXI4Parameters.RESP_OKAY
  in.b.valid := state===s_wresp

  in.b.bits.id := RegEnable(in.aw.bits.id, in.aw.fire)
  in.b.bits.user := RegEnable(in.aw.bits.user, in.aw.fire)
  in.r.bits.id := RegEnable(in.ar.bits.id, in.ar.fire)
  in.r.bits.user := RegEnable(in.ar.bits.user, in.ar.fire)
}
