package mips.config

import chisel3._
import chisel3.experimental.EnumFactory
import chisel3.util.MuxLookup

//class MyMux(){
//
//}

/**
 * 默认输出为Array中的第一项 the default value is Array(0)._2
 */
object MyMux {
  def apply[S <: UInt, T <: Data](sel: S, array: Array[(S, T)]): Data = {
    val arrayUInt = array.map { case (s, t) => {
      s.asUInt -> t
    }
    }.tail
    MuxLookup(sel.asUInt, array(0)._2, arrayUInt)
  }

  def apply[S <: Data, T <: Data, M <: chisel3.experimental.EnumType](sel: S, array: Array[(M, T)]): Data = {
    val arrayUInt = array.map { case (s, t) => {
      s.asUInt -> t
    }
    }.tail
    MuxLookup(sel.asUInt, array(0)._2, arrayUInt)
  }

  def apply[S <: Data, T <: Data, M <: chisel3.experimental.EnumType](sel: S, default: S, array: Array[(M, T)]): Data = {
    val arrayUInt = array.map { case (s, t) => {
      s.asUInt -> t.asUInt
    }
    }
    MuxLookup(sel.asUInt, default.asUInt, arrayUInt)
  }
}

/**
 * 带有优先级的多路选择器，mapping里是(条件 -> 值)的优先级降序排列 a mux with priority, mapping is organized by
 * (condition -> value) in descending order of priority
 */
object MyMuxWithPriority {
  // todo 写的比较垃圾，没有掌握函数式编程的精髓，有待优化，理论上可以直接用reduce实现。
  def apply[S <: Bool, T <: Data](default: T, mapping: Seq[(S, T)]): T = {
    def mux(mapping: Seq[(S, T)]): T = {
      val (s, t) = mapping(0)
      if (mapping.length == 1) {
        Mux(s, t, default)
      } else {
        Mux(s, t, mux(mapping.tail))
      }
    }

    mux(mapping)
  }
}

object SoftMuxByConfig {
  // 使用传名调用，可以避免get方法报错
  def apply[S <: Boolean, T <: Data](sel: S, hasSel: => T, noSel: => T): T = {
    if (sel) {
      hasSel
    } else {
      noSel
    }
  }
  def apply[S <: Boolean, T <: Data](sel: S, hasSel: => Seq[(T, T)], noSel: => Seq[(T, T)]): Seq[(T, T)] = {
    if (sel) {
      hasSel
    } else {
      noSel
    }
  }
}
