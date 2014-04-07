package nbbmq.akka.mailbox

import nbbmq.akka.util.AtomicCounter
import java.util.concurrent.ConcurrentLinkedQueue

class NonBlockingBoundedQueue[E](val capacity: Long) {

  val currentCount = new AtomicCounter(0)
  private val queue = new ConcurrentLinkedQueue[E]()

  def isEmpty = queue.isEmpty
  def count() = queue.size()

  def poll(): E = {
    val e = queue.poll()
    if(e != null) { currentCount.addOrdered(-1L) }
    e
  }

  def add(handle: E) = {
    queue.add(handle)
    currentCount.addOrdered(1L)
    resizeQueueMaybe()
  }

  def resizeQueueMaybe(): Unit = {
    var value = 0L
    do {
      value = currentCount.get()
      if(value > capacity){
        val e = poll()
        //queue is most likely empty. This is a good time to bring in the consistency
        //so the current count matches the queue size
        if(e == null) {
          println(s"Bring consistency...${Thread.currentThread().getName} - ${System.currentTimeMillis()}")
          auditCount()
        } else { dropMessage(e) }
      }
    } while(value > capacity)
  }

  private def auditCount() = {
    currentCount.setOrdered(0L)
  }

  def dropMessage(e: E) = { /* do nothing */ }
}
