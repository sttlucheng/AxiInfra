// SPDX-License-Identifier: MulanPSL-2.0
// Copyright (c) 2025-2026 RedRISC Technology Co. Ltd.

package xs.infra.axi

import chisel3._
import chisel3.util._

class AxiErrorDevice(axiParams: AxiParams) extends Module {
  require(axiParams.lastBits != 0)
  val io = IO(new Bundle {
    val axi = Flipped(new AxiBundle(axiParams))
  })
  io.axi.w.ready := true.B

  io.axi.b.valid     := io.axi.aw.valid
  io.axi.aw.ready    := io.axi.b.ready
  io.axi.b.bits.id   := io.axi.aw.bits.id
  io.axi.b.bits.user := io.axi.aw.bits.user
  io.axi.b.bits.resp := "b11".U

  io.axi.r.bits.data := DontCare
  io.axi.r.bits.resp := "b11".U
  if(axiParams.lenBits == 0) {
    io.axi.r.valid     := io.axi.ar.valid
    io.axi.ar.ready    := io.axi.r.ready
    io.axi.r.bits.id   := io.axi.ar.bits.id
    io.axi.r.bits.last := 1.U
    io.axi.r.bits.user := io.axi.ar.bits.user
  } else {
    val arv = RegInit(false.B)
    val arc = Reg(UInt(axiParams.lenBits.W))
    val ari = RegEnable(io.axi.ar.bits.id, io.axi.ar.fire)
    val aru = RegEnable(io.axi.ar.bits.user, io.axi.ar.fire)

    io.axi.ar.ready := !arv
    when(io.axi.ar.fire) {
      arv := true.B
    }.elsewhen(io.axi.r.fire && io.axi.r.bits._last) {
      arv := false.B
    }

    when(io.axi.ar.fire) {
      arc := io.axi.ar.bits.len
    }.elsewhen(io.axi.r.fire) {
      arc := arc - 1.U
    }

    io.axi.r.valid     := arv
    io.axi.r.bits.id   := ari
    io.axi.r.bits.last := (arc === 0.U).asUInt
    io.axi.r.bits.user := aru
  }

}
