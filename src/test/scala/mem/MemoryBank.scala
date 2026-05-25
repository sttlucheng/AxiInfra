// SPDX-License-Identifier: MulanPSL-2.0
// Copyright (c) 2025-2026 RedRISC Technology Co. Ltd.

package mem

import chisel3._
import chisel3.util._
import freechips.rocketchip.amba.axi4.{AXI4MasterNode, AXI4MasterParameters, AXI4MasterPortParameters}
import freechips.rocketchip.diplomacy.{AddressSet, IdRange}
import xs.infra.axi.{AxiBundle, AxiParams, ExtAxiBundle}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule.{LazyModule, LazyModuleImp}

class MemoryBank(port: AxiParams)(implicit p:Parameters) extends LazyModule {
  private val memDplmcMstParams = AXI4MasterPortParameters(
    masters = Seq(
      AXI4MasterParameters(
        name = "mem",
        id = IdRange(0, 1 << port.idBits)
      )
    )
  )
  private val mstNode = AXI4MasterNode(Seq(memDplmcMstParams))
  private val ram = LazyModule(new AXI4RAM(
    address = Seq(AddressSet(0x0L, (0x1L << port.addrBits) - 1)),
    memByte = 0x1L << port.addrBits,
    executable = true,
    beatBytes = port.dataBits / 8,
    burstLen = 4096 * 8 / port.dataBits
  ))
  ram.node := mstNode
  lazy val module = new Impl

  class Impl extends LazyModuleImp(this) {
    val s_axi = IO(Flipped(new ExtAxiBundle(port)))

    private def connectByName[T <: Bundle, K <: Bundle](sink: ReadyValidIO[T], src: ReadyValidIO[K]):Unit = {
      sink.valid := src.valid
      src.ready := sink.ready
      sink.bits := DontCare
      val recvMap = sink.bits.elements.map(e => (e._1.toLowerCase, e._2))
      val sendMap = src.bits.elements.map(e => (e._1.toLowerCase, e._2))
      for((name, data) <- recvMap) {
        if(sendMap.contains(name)) data := sendMap(name).asTypeOf(data)
      }
    }
    private val _s_axi = Wire(new AxiBundle(port))
    _s_axi <> s_axi
    connectByName(mstNode.out.head._1.aw, _s_axi.aw)
    connectByName(mstNode.out.head._1.ar, _s_axi.ar)
    connectByName(mstNode.out.head._1.w, _s_axi.w)
    connectByName(_s_axi.r, mstNode.out.head._1.r)
    connectByName(_s_axi.b, mstNode.out.head._1.b)
  }
}
