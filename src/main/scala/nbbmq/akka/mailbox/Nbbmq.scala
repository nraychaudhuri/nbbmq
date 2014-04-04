package nbbmq.akka.mailbox

import akka.dispatch._
import akka.actor.{ActorSystem, ActorRef}
import com.typesafe.config.Config
import nbbmq.akka.util.AtomicCounter

trait NonBlockingBoundedMessageQueueSemantics {
  def capacity: Long
}

object NonBlockingBoundedMailbox {
  
  class NonBlockingBoundedMessageQueue(val capacity: Long) extends AbstractNodeQueue[Envelope]
    with MessageQueue with NonBlockingBoundedMessageQueueSemantics {

    val runningCount: AtomicCounter = new AtomicCounter(0)

    override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
      while(hasMessages) { deadLetters.enqueue(owner, dequeue())}
    }

    override def hasMessages: Boolean = !isEmpty
    override def numberOfMessages: Int = count()
    override def dequeue(): Envelope = poll()

    override def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
      add(handle)
      var value = runningCount.addAndGet(1)
      do {
        if(value > capacity){
          poll() //dropping messages
          value = runningCount.addAndGet(-1)
        }
      } while(value > capacity)
    }
  }
}

class NonBlockingBoundedMailbox(val capacity: Long) extends MailboxType with ProducesMessageQueue[NonBlockingBoundedMailbox.NonBlockingBoundedMessageQueue] {

  import NonBlockingBoundedMailbox._

  // This constructor signature must exist, it will be called by Akka
  def this(settings: ActorSystem.Settings, config: Config) = {
    this(config.getLong("mailbox-capacity"))
  }

  // The create method is called to create the MessageQueue
  final override def create(owner: Option[ActorRef],
                            system: Option[ActorSystem]): MessageQueue =
    new NonBlockingBoundedMessageQueue(capacity)
}
