package nbbmq.akka.mailbox

import akka.dispatch._
import akka.actor.{Scheduler, ActorSystem, ActorRef}
import com.typesafe.config.Config
import nbbmq.akka.util.AtomicCounter


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

class NonBlockingBoundedMailbox(val capacity: Long, queueAuditDuration: Long, dropMessagesToDeadLetters: Boolean)
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
