import akka.actor.{Props, ActorSystem, Actor}
import akka.dispatch.RequiresMessageQueue
import com.typesafe.config.ConfigFactory
import nbbmq.akka.mailbox.NonBlockingBoundedMessageQueueSemantics

object Main extends App {

  class MySpecialActor extends Actor with RequiresMessageQueue[NonBlockingBoundedMessageQueueSemantics] {
    def receive = {
      case message =>
        Thread.sleep(100)
        println(">>>> Processing..." + message)
    }
  }


  val system = ActorSystem("test", ConfigFactory.parseString(
    """
      |nonblocking-bounded-mailbox {
      |  mailbox-type = "nbbmq.akka.mailbox.NonBlockingBoundedMailbox"
      |  mailbox-capacity = 5
      |}
      |
      |akka.actor.mailbox.requirements {
      |  "nbbmq.akka.mailbox.NonBlockingBoundedMessageQueueSemantics" = nonblocking-bounded-mailbox
      |}
    """.stripMargin))

  val ref = system.actorOf(Props[MySpecialActor], "foo")

   //Seq(1, 2, 3, 4, 5, 6).foreach(ref ! _)
  Seq('a' to 'z', 1 to 100, 200 to 300).par.foreach(_.foreach(ref ! _))

  System.in.read()
  system.shutdown()
  system.awaitTermination()
}
