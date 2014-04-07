package nbbmq.akka.mailbox

import org.scalatest._

class NonBlockingBoundedMessageQueueSpec extends FlatSpec with Matchers {

  def queue(capacity: Int, elements:String*) = {
    val q = new NonBlockingBoundedQueue[String](capacity)
    elements.foreach(q.add(_))
    q
  }

  "Non-blocking bounded queue" should "drop messages when it reaches capacity" in {
    val q = queue(3, "foo", "bar", "baz", "jazz")
    q.count() should be (3)
    q.currentCount.get() should be (3)
  }

  it should "drop messages in first in first our order" in {
    val q = queue(3, "foo", "bar", "baz", "jazz")
    q.poll() should be ("bar")
    q.poll() should be ("baz")
    q.poll() should be ("jazz")
    q.count() should be (0)
    q.currentCount.get() should be (0)
  }

  it should "not decrement current count when queue is empty" in {
    val q = queue(3, "foo")
    q.poll()
    q.poll()
    q.poll()

    q.currentCount.get() should be (0)
    q.count() should be (0)
  }
}
