package util

trait ComposableFunction1[-T1, +R] {
  val f: T1 => R
  def >>[A](g: R => A): T1 => A = f andThen g
  def <<[A](g: A => T1): A => R = f compose g

}

trait Composable {
  // composeの組み合わせを、<</>>で制御できるようにする
  implicit def toComposableFunction1[T1, R](func: T1 => R) = new ComposableFunction1[T1, R] { val f = func }
}
