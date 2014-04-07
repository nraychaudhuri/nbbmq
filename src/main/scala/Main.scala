import akka.actor.{Props, ActorSystem, Actor}
import akka.dispatch.RequiresMessageQueue
import com.typesafe.config.ConfigFactory
import nbbmq.akka.mailbox.{NonBlockingBoundedQueue, NonBlockingBoundedMessageQueueSemantics}

object Main extends App {

  class MySpecialActor extends Actor with RequiresMessageQueue[NonBlockingBoundedMessageQueueSemantics] {
    def receive = {
      case message =>
        Thread.sleep(10)
        if(message == null) println("Received null message")
        //println(">>>> Processing..." + message)
    }
  }


  val system = ActorSystem("test", ConfigFactory.parseString(
    """
      |nonblocking-bounded-mailbox {
      |  mailbox-type = "nbbmq.akka.mailbox.NonBlockingBoundedMailbox"
      |  mailbox-capacity = 5000
      |  drop-messages-to-dead-letters = true
      |}
      |
      |akka.actor.mailbox.requirements {
      |  "nbbmq.akka.mailbox.NonBlockingBoundedMessageQueueSemantics" = nonblocking-bounded-mailbox
      |}
    """.stripMargin))

  val ref = system.actorOf(Props[MySpecialActor], "foo")

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  system.scheduler.schedule(2 milliseconds, 50 milliseconds) {
    Seq(1 to 10000, 1 to 10000, 1 to 10000, 1 to 10000, 1 to 10000, 1 to 10000, 1 to 10000, 1 to 10000, 1 to 10000, 1 to 10000).par.foreach(_.foreach(ref ! _))
  }

//  val q = new NonBlockingBoundedQueue[Any](50)
//  system.scheduler.schedule(2 milliseconds, 50 milliseconds) {
//    Seq('a' to 'z', 1 to 100, 200 to 300, 1000 to 2000).par.foreach(q.add(_))
//  }

   //Seq(1, 2, 3, 4, 5, 6).foreach(ref ! _)

  System.in.read()
  system.shutdown()
  system.awaitTermination()
}
