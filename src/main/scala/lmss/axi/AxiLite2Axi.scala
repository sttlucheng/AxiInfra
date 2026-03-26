package lmss.axi

import chisel3._
import chisel3.util._

class AxiLite2Axi(axiParams: AxiParams) extends Module {
  require(axiParams.lastBits == 0)
  private val slvP = AxiParams(
    addrBits = axiParams.addrBits,
    idBits = 1,
    dataBits = axiParams.dataBits,
    attr = axiParams.attr
  )
  val io = IO(new Bundle {
    val mst = Flipped(new AxiBundle(axiParams))
    val slv = new AxiBundle(slvP)
  })

  io.slv.aw.valid       := io.mst.aw.valid
  io.slv.aw.bits.id     := 0.U
  io.slv.aw.bits.addr   := io.mst.aw.bits.addr
  io.slv.aw.bits.len    := 0.U
  io.slv.aw.bits.size   := log2Ceil(axiParams.dataBits / 8).U
  io.slv.aw.bits.burst  := 0.U
  io.slv.aw.bits.lock   := 0.U
  io.slv.aw.bits.cache  := 0.U
  io.slv.aw.bits.prot   := io.mst.aw.bits.prot
  io.slv.aw.bits.qos    := 0.U
  io.slv.aw.bits.region := 0.U
  io.slv.aw.bits.user   := io.mst.aw.bits.user
  io.mst.aw.ready       := io.slv.aw.ready

  io.slv.ar.valid       := io.mst.ar.valid
  io.slv.ar.bits.id     := 0.U
  io.slv.ar.bits.addr   := io.mst.ar.bits.addr
  io.slv.ar.bits.len    := 0.U
  io.slv.ar.bits.size   := log2Ceil(axiParams.dataBits / 8).U
  io.slv.ar.bits.burst  := 0.U
  io.slv.ar.bits.lock   := 0.U
  io.slv.ar.bits.cache  := 0.U
  io.slv.ar.bits.prot   := io.mst.ar.bits.prot
  io.slv.ar.bits.qos    := 0.U
  io.slv.ar.bits.region := 0.U
  io.slv.ar.bits.user   := io.mst.ar.bits.user
  io.mst.ar.ready       := io.slv.ar.ready

  io.slv.w.valid     := io.mst.w.valid
  io.slv.w.bits.data := io.mst.w.bits.data
  io.slv.w.bits.strb := io.mst.w.bits.strb
  io.slv.w.bits.last := 1.U
  io.slv.w.bits.user := io.mst.w.bits.user
  io.mst.w.ready     := io.slv.w.ready

  io.mst.r <> io.slv.r
  io.mst.b <> io.slv.b
}
