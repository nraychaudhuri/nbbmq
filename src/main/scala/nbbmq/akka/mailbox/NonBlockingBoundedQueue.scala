package nbbmq.akka.mailbox

import nbbmq.akka.util.AtomicCounter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import scala.annotation.tailrec
import akka.util.Unsafe
import scala.reflect.ClassTag

/**
 * Non-blocking bound queue implementation for Akka mailbox where it is ok to drop old messages. Internally the queue
 * is backed by an ring buffer where if queue is full old messages are overridden by new messagesa and head always
 * points to the oldest available message in the queue.
 *
 * @param capacity the size of the queue. Make sure this value == 2 pow n
 * @tparam A Type of element it queues
 */
class NonBlockingBoundedQueue[A:ClassTag](capacity: Int) {

  private val unsafe = Unsafe.instance
  private val arrayBase = unsafe.arrayBaseOffset(classOf[Array[Any]])
  private val arrayScale = calculateShiftForScale(unsafe.arrayIndexScale(classOf[Array[Any]]))

  //Make sure the capacity is some value that is power of 2. This important for the efficient bitwise module
  //calculation.
  val actualCapacity = findNextPositivePowerOfTwo(capacity)
  private val ringSize = actualCapacity - 1
  private val buffer = Array.ofDim[A](actualCapacity)

  private val head = new AtomicCounter(0)
  private val tail = new AtomicCounter(0)

  def offer(elem: A): Boolean = {
    var currentTail: Long = 0L
    do {
      currentTail = tail.get
    } while (!tail.compareAndSet(currentTail, currentTail + 1))

    //checking whether we are overriding oldest queue element
    var currentHead = head.get
    if (currentTail >= (currentHead + actualCapacity)) {
      //move the head to the next oldest message in the queue
      val newHead = currentTail - ringSize
      //avoiding possible race here to make sure that we don't move head backward
      while(newHead > currentHead && !head.compareAndSet(currentHead, newHead)) {
        currentHead = head.get()
      }
    }
    val index: Int = currentTail.asInstanceOf[Int] & ringSize
    unsafe.putOrderedObject(buffer, calculateOffset(index), elem)

    return true
  }

  def poll(): A = {
    var currentHead: Long = 0L
    val currentTail = tail.get()
    do {
      currentHead = head.get();
      if (currentHead >= currentTail) {
        return null.asInstanceOf[A];
      }
    } while (!head.compareAndSet(currentHead, currentHead + 1));

    val index: Int = currentHead.asInstanceOf[Int] & ringSize
    getElementVolatile(index)
  }

  def isEmpty: Boolean = tail.get() == head.get()

  def size(): Int = {
    val size = tail.get() - head.get()
    return size.toInt
  }

  private def getElementVolatile(index: Int): A = {
    return unsafe.getObjectVolatile(buffer, calculateOffset(index)).asInstanceOf[A]
  }

  private def calculateOffset(index: Int): Long = {
    return arrayBase + (index.asInstanceOf[Long] << arrayScale)
  }

  // How many times should a value be shifted left for a given scale of pointer.
  private def calculateShiftForScale(scale: Int) = scale match {
    case 4 => 2
    case 8 => 3
    case _ => throw new IllegalStateException("Unknown pointer size")
  }

  //ex: 4 => 4, 5, => 8, 10 => 16
  def findNextPositivePowerOfTwo(value: Int): Int =  1 << (32 - Integer.numberOfLeadingZeros(value - 1))
}
