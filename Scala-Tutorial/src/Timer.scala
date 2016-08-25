

object Timer {
  def oncePerSecond(callback: () => Unit) {
    while (true) {callback(); Thread sleep 1000 }
  }
  
  // Named callback
  def timeFlies() {
    println("time flies like an arrow...")
  }
  
  // Anonymous callback
  def main(args: Array[String]) {
    oncePerSecond(() =>
      println("time files like an arrow..."))
  }
}