

object ComplexNumbers {
  def main(args: Array[String]) {
    val c = new Complex(1.2, 3.4)
    // Call with parens
    println("imaginary part: " + c.im())
    // Call without parens
    println("real part: " + c.re)
  }
}