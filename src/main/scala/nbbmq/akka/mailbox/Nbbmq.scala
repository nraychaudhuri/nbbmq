package nbbmq.akka.mailbox

import akka.dispatch._
import akka.actor.{ActorSystem, ActorRef}
import com.typesafe.config.Config


/**
 * Defining semantics of non-blocking bounded queue where oldest messages are dropped from the queue eventually when it grows
 * out of the given capacity.The common use case of these kind of queues are processing real-time events where its ok to drop
 * old events (messages) when queue is backed up.
 */
trait NonBlockingBoundedMessageQueueSemantics {
  def capacity: Int
}

object NonBlockingBoundedMailbox {
  
  class Nbbmq(val capacity: Int) extends MessageQueue with NonBlockingBoundedMessageQueueSemantics {

    val queue = new NonBlockingBoundedQueue[Envelope](capacity)
    override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
      while(hasMessages) { deadLetters.enqueue(owner, dequeue())}
    }

    override def hasMessages: Boolean = !queue.isEmpty
    override def numberOfMessages: Int = queue.size()
    override def dequeue(): Envelope = queue.poll()
    override def enqueue(receiver: ActorRef, handle: Envelope): Unit =  queue.offer(handle)
  }
}

/**
 * Akka Mailbox implementation of Non-blocking bounded message queue.
 *
 * @param capacity the approx capacity of the queue
 */
class NonBlockingBoundedMailbox(val capacity: Int)
  extends MailboxType with ProducesMessageQueue[NonBlockingBoundedMailbox.Nbbmq] {

  import NonBlockingBoundedMailbox._

  def this(settings: ActorSystem.Settings, config: Config) = {
    this(config.getInt("mailbox-capacity"))
  }

  final override def create(owner: Option[ActorRef],
                            system: Option[ActorSystem]): MessageQueue = {
    new Nbbmq(capacity)
  }
}
