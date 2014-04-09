package nbbmq.akka.mailbox

import org.scalatest._

class NonBlockingBoundedMessageQueueSpec extends FlatSpec with Matchers {

  def queue(capacity: Int, elements:String*) = {
    val q = new NonBlockingBoundedQueue[String](capacity)
    elements.foreach(q.offer(_))
    q
  }

  "Non-blocking bounded queue" should "drop messages when it reaches capacity" in {
    val q = queue(4, "foo", "bar", "baz", "jazz", "caz")
    q.size() should be (4)
  }

  it should "not drop messages when it can fit them" in {
    val q = queue(3, "foo", "bar", "baz")
    q.poll() should be ("foo")
    q.poll() should be ("bar")
    q.poll() should be ("baz")
    q.size() should be (0)
  }

  it should "drop messages in first in first our order" in {
    val q = queue(4, "foo", "bar", "baz", "jazz", "caz")
    q.poll() should be ("bar")
    q.poll() should be ("baz")
    q.poll() should be ("jazz")
    q.poll() should be ("caz")
    q.size() should be (0)
  }

  it should "make sure head always read the oldest message in the queue" in {
    //the queue is filled more than twice
    val values = 1 to 35 map (_.toString)
    val q = queue(8, values: _*)
    q.size() should be (8)
    q.poll() should be ("28")
    q.poll() should be ("29")
    q.poll() should be ("30")
    q.poll() should be ("31")
    q.poll() should be ("32")
    q.size() should be (3)
  }
}
