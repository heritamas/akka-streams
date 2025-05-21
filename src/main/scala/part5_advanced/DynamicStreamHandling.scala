package part5_advanced

import akka.actor.ActorSystem
import akka.actor.TypedActor.context
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitches}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object DynamicStreamHandling extends App {

  implicit val system = ActorSystem("DynamicStreamHandling")
  // this line needs to be here for Akka < 2.6
  // implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  // #1: Kill Switch

  val killSwitchFlow = KillSwitches.single[Int]
  val counter = Source(LazyList.from(1)).throttle(1, 1.second).log("counter")
  val sink = Sink.ignore

    val killSwitch = counter
      .viaMat(killSwitchFlow)(Keep.right)
      .to(sink)
      //.run()

//    system.scheduler.scheduleOnce(3 seconds) {
//      killSwitch.shutdown()
//    }


  // shared kill switch
  val anotherCounter = Source(LazyList.from(1)).throttle(2, 1.second).log("anotherCounter")
  val sharedKillSwitch = KillSwitches.shared("oneButtonToRuleThemAll")

//  counter.via(sharedKillSwitch.flow).runWith(Sink.ignore)
//  anotherCounter.via(sharedKillSwitch.flow).runWith(Sink.ignore)
//
//  system.scheduler.scheduleOnce(3 seconds) {
//    sharedKillSwitch.shutdown()
//  }

  // MergeHub

  val dynamicMerge = MergeHub.source[Int]
  val materializedSink = dynamicMerge.to(Sink.foreach[Int](println)).run()

  // use this sink any time we like
//  Source(1 to 10).runWith(materializedSink)
//  counter.runWith(materializedSink)

  // BroadcastHub

  val dynamicBroadcast = BroadcastHub.sink[Int](bufferSize = 256)
  val materializedSource = Source(1 to 100).throttle(10, 100 millis).log("to 100").runWith(dynamicBroadcast)

  val ignored = materializedSource.log("to be ignored").runWith(Sink.ignore)
  val printed = materializedSource.log("to be printed").runWith(Sink.foreach[Int](e => println(s"I received: $e")))

  for {
    _ <- ignored
    _ <- printed
  } yield println("All streams completed.")

  Await.result(ignored, Duration.Inf)
  Await.result(printed, Duration.Inf)

  /**
    * Challenge - combine a mergeHub and a broadcastHub.
    *
    * A publisher-subscriber component
    */
//  val merge = MergeHub.source[String]
//  val bcast = BroadcastHub.sink[String]
//  val (publisherPort, subscriberPort) = merge.toMat(bcast)(Keep.both).run()
//
//  subscriberPort.runWith(Sink.foreach(e => println(s"I received: $e")))
//  subscriberPort.map(string => string.length).runWith(Sink.foreach(n => println(s"I got a number: $n")))
//
//  Source(List("Akka", "is", "amazing")).runWith(publisherPort)
//  Source(List("I", "love", "Scala")).runWith(publisherPort)
//  Source.single("STREEEEEEAMS").runWith(publisherPort)

}
