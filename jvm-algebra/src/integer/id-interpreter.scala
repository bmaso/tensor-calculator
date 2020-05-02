package bmaso.tensoralg.jvm.integer

import scala.language.postfixOps

import cats.{~>, Id}
import cats.implicits._

import bmaso.tensoralg.abstractions.Dimension
import IntTensorAlgebra._

/**
 * Note: split operation evaluator assumes split step evenly divides
 * magnitude in the same dimension of original tensor. Code constructing the
 * Split operation needs to assert this assumption.
 **/
object IdInterpreter extends (TensorExprOp ~> Id) {

  def eval[A](expr: TensorExpr[A]): Id[A] = expr.foldMap(this)

  override def apply[A](expr: TensorExprOp[A]): Id[A] = expr match {
    case TensorFromArray(array, magnitude, offset) =>
      IntArrayTensor(array, magnitude, offset)

    case CopyTensorElementsToArray(tensor, targetArray, targetOffset) => tensor match {
      case IntArrayTensor(array, magnitude, offset) =>
        Array.copy(array, offset, targetArray, targetOffset, tensor.elementSize.toInt)
        tensor

      case t: IntTensor =>
        for(idx <- 0 to t.elementSize.toInt - 1) {
          targetArray(idx + targetOffset) = t.valueAt1D(idx)
        }
        t
    }

    case Translate(tensor: IntTensor, offsets: Array[Long]) =>
      TranslateTensor(tensor: IntTensor, offsets: Array[Long])

    case Broadcast(tensor: IntTensor, dimension: Dimension, _magnitude: Long) =>
      BroadcastTensor(tensor, dimension, _magnitude)

    case Slice(tensor: IntTensor, sliceRange: Array[(Long, Long)]) =>
      SliceTensor(tensor, sliceRange)

    case Reshape(tensor: IntTensor, reshapedMagnitude: Array[Long]) =>
      ReshapeTensor(tensor, reshapedMagnitude)

    case Join(tensors: List[IntTensor], joiningDimension) =>
      //...use a StackTensor to represent joining if the source tensors are unitary
      //   in the join dimension -- StackTensor is much more efficient in space and
      //   access time compared to JoinTensor because we know join dimension is unitary...
      //...when source tensors are not all unitary in join dimension, then use JoinTensor.
      //   JoinTensor has the added capability to stack tensors of uneven size in the
      //   join dimension, but is less efficient in space and element value
      //   access time compared to StackTensor...

      if(!tensors.map(_.magnitudeIn(joiningDimension) > 1).fold(false)(_ || _))
        StackTensor(tensors.toArray, joiningDimension)
      else
        JoinTensor(tensors.toArray, joiningDimension)

    case Reverse(tensor: IntTensor, dimension: Dimension) =>
      ReverseTensor(tensor, dimension)

    case Pivot(tensor: IntTensor, dim1: Dimension, dim2: Dimension) =>
      PivotTensor(tensor, dim1, dim2)

    case Map(tensor: IntTensor, map_f: IntMapFunction) =>
      MapTensor(tensor, map_f.f)

    case Reduce(tensor: IntTensor, reduceOrders: Int, reduce_f: IntReduceFunction) =>
      ReduceTensor(tensor, reduceOrders, reduce_f.f)

    case IntTensorAlgebra.Unit => ()
  }
}
