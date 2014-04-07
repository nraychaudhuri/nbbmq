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
  def capacity: Long
}

object NonBlockingBoundedMailbox {
  
  class Nbbmq(capacity: Long, deadLetters: Option[ActorRef]) extends NonBlockingBoundedQueue[Envelope](capacity)
    with MessageQueue with NonBlockingBoundedMessageQueueSemantics {

    override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
      while(hasMessages) { deadLetters.enqueue(owner, dequeue())}
    }
    override def hasMessages: Boolean = !isEmpty
    override def numberOfMessages: Int = count()

    override def dequeue(): Envelope = { poll() }

    override def enqueue(receiver: ActorRef, handle: Envelope): Unit = { add(handle) }

    override def dropMessage(e: Envelope) = {
      deadLetters.foreach(_ ! e)
    }
  }
}

/**
 * Akka Mailbox implementation of Non-blocking bounded message queue.
 *
 * @param capacity the approx capacity of the queue
 * @param dropMessagesToDeadLetters set to true if you want to send drop messages to dead letters
 */
class NonBlockingBoundedMailbox(val capacity: Long, dropMessagesToDeadLetters: Boolean)
  extends MailboxType with ProducesMessageQueue[NonBlockingBoundedMailbox.Nbbmq] {

  import NonBlockingBoundedMailbox._

  def this(settings: ActorSystem.Settings, config: Config) = {
    this(config.getLong("mailbox-capacity"),
      config.getBoolean("drop-messages-to-dead-letters"))
  }

  final override def create(owner: Option[ActorRef],
                            system: Option[ActorSystem]): MessageQueue = {
    val deadLetters = if(dropMessagesToDeadLetters) system.map(_.deadLetters) else None
    new Nbbmq(capacity, deadLetters)
  }
}
