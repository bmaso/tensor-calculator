package bmaso.tensoralg.jvm.integer

import bmaso.tensoralg.abstractions.{Tensor => abstract_Tensor, Dimension}

sealed trait IntTensor extends abstract_Tensor {
  def valueAt(index: Array[Int]): Int
  def v(index: Array[Int]) = valueAt(index)
  def valueAt1D(_1dIdx: Int): Int = {
    var __1dIdx = _1dIdx
    val idx = Array.fill[Int](order)(0)
    for(d <- order-1 to 0 by -1;
        m = magnitude(d)) {
      idx(d) = __1dIdx % m
      __1dIdx = __1dIdx / m
    }
    valueAt(idx)
  }
}

case class IntArrayTensor(arr: Array[Int], override val magnitude: Array[Int], offset: Int, length: Int)
    extends IntTensor {
  if(arr.length != this.elementSize) throw new IllegalArgumentException

  override def valueAt(index: Array[Int]): Int = {
    var idx = 0
    var multiplier = 1
    for(ii <- (index.length - 1) to 0 by -1) {
      idx += index(ii) * multiplier
      multiplier *= magnitude(ii)
    }
    arr(idx + offset)
  }
}

case class TranslateTensor(tensor: IntTensor, offsets: Array[Long])
    extends IntTensor {
  override def magnitude = tensor.magnitude
  override def valueAt(index: Array[Int]): Int = {
    val translatedIndex = Array.copyAs[Int](index, index.length)
    var oob = false
    for(d <- 0 to order - 1) {
      translatedIndex(d) -= offsets(d).toInt
      if(translatedIndex(d) < 0 || translatedIndex(d) >= magnitude(d)) oob = true
    }
    if(oob) 0 else tensor.valueAt(Array.copyAs[Int](translatedIndex, translatedIndex.length))
  }
}
