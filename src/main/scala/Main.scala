import akka.actor.{Props, ActorSystem, Actor}
import akka.dispatch.RequiresMessageQueue
import com.typesafe.config.ConfigFactory
import java.util.concurrent.atomic.AtomicLong
import nbbmq.akka.mailbox.{NonBlockingBoundedQueue, NonBlockingBoundedMessageQueueSemantics}

object Main extends App {
  class MySpecialActor extends Actor with RequiresMessageQueue[NonBlockingBoundedMessageQueueSemantics] {
    var lastMessage: Long = 0L
    def receive = {
      case message: Long =>
        Thread.sleep(10)
        //println(">>>> Processing..." + message)
    }
  }


  val system = ActorSystem("test", ConfigFactory.parseString(
    """
      |nonblocking-bounded-mailbox {
      |  mailbox-type = "nbbmq.akka.mailbox.NonBlockingBoundedMailbox"
      |  mailbox-capacity = 64
      |}
      |
      |akka.actor.mailbox.requirements {
      |  "nbbmq.akka.mailbox.NonBlockingBoundedMessageQueueSemantics" = nonblocking-bounded-mailbox
      |}
    """.stripMargin))

  val ref = system.actorOf(Props[MySpecialActor], "testActor")

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global


  val counter = new AtomicLong(0)
  system.scheduler.schedule(2 milliseconds, 50 milliseconds) {
    Seq(1 to 10000,
      1 to 10000,
      1 to 10000,
      1 to 10000).par.foreach( _.foreach{m =>
        val counter1 = counter.getAndIncrement()
        println(s"Sending...${counter1}")
        ref ! counter1
      })
  }

//  val q = new NonBlockingBoundedQueue[Int](8)
//    system.scheduler.schedule(2 milliseconds, 1000 * 2 milliseconds) {
//      Seq(1 to 32, 1 to 32, 1 to 32, 1 to 32).par.map(r => r.foreach(q.offer(_))).toSeq
//      println(">>>>> " + q.size())
//    }
//
//  val q1 = new NonBlockingBoundedQueue[Int](64)
//  system.scheduler.schedule(2 milliseconds, 50 milliseconds) {
//    Seq(1 to 100, 200 to 300, 1000 to 2000, 4000 to 5000).par.foreach(r => r.foreach(q1.offer(_)))
//    println(">>>>> " + q.size())
//  }
//
//Seq(1, 2, 3, 4, 5, 6).foreach(ref ! _)

  System.in.read()
  system.shutdown()
  system.awaitTermination()
}
