package nbbmq.akka.mailbox

import nbbmq.akka.util.AtomicCounter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import scala.annotation.tailrec

/**
 * Non-blocking bound queue implementation for Akka mailbox where it is ok to drop old messages. It uses capacity to
 * keep a check on the size of the queue. Because of the asynchronous nature of these queues, determining the current
 * number of elements is not feasible so maintaining exact capacity size is not possible. Instead it is using an efficient
 * an atomic counter(Unsafe.putOrderedLong) to determine the approx size(could be off from the real size) and
 * drop messages from head of the queue to balance the size.
 *
 *
 * @param capacity The approx size that you want to maintain for the queue
 * @tparam E Type of element it queues
 */
class NonBlockingBoundedQueue[E](val capacity: Long) {

  //running count of number of elements inserted in a queue
  private[mailbox] val currentCount = new AtomicCounter(0)

  //cleanup flag so that one thread can take the responsibility of drop messages
  //if the queue grows out of size. This is required so that we don't run the cleanup on
  //multiple threads and drain the queue.
  private[mailbox] val cleaningNow = new AtomicBoolean(false)

  private val queue = new ConcurrentLinkedQueue[E]()

  def isEmpty = queue.isEmpty
  def count() = queue.size()

  def poll(): E = {
    val e = queue.poll()
    //should perform better than volatile write. The trade of is not all threads will see the updated
    //value immediately
    if(e != null) { currentCount.addOrdered(-1L) }
    e
  }

  def add(handle: E) = {
    queue.add(handle)
    currentCount.addOrdered(1L)  //using Unsafe.putOrderedLong for better performance
    resizeQueueMaybe()
  }

  def resizeQueueMaybe(): Unit = {
    val value = currentCount.get()
    //only do clean up when we grow out of capacity and nobody else
    //has started it
    //TODO: We do have a additional volatile read here. I wish there was
    //a more efficient way to doing it
    if(value > capacity && !cleaningNow.get()) {
      cleaningNow.set(true)
      resizeQueue(value)
      cleaningNow.set(false)
    }
  }

  @tailrec
  private def resizeQueue(count: Long): Unit = {
    if(count <= capacity)
      ()
    else {
      dropMessageMaybe()
      resizeQueue(currentCount.get())
    }
  }

  private def dropMessageMaybe() = {
    val e = poll()
    //most likely the queue is empty. Lets use this
    // as a opportunity to correct the running total
    if(e == null)
       auditCount()
    else
      dropMessage(e)
  }

  private def auditCount() = currentCount.setOrdered(0L)

  def dropMessage(e: E) = { /* do nothing */ }
}
