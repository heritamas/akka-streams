package playground

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

object BroadcastHubExample extends App {
  implicit val system = ActorSystem("BroadcastHubDemo")
  implicit val materializer = Materializer(system)
  import system.dispatcher

  // Set up the BroadcastHub (shared sink)
  val broadcastSink: Sink[Int, Source[Int, NotUsed]] = BroadcastHub.sink(bufferSize = 256)

  // Lazy source: will only emit once subscribers are attached
  val (queue, broadcastSource) = Source.queue[Int](bufferSize = 10).toMat(broadcastSink)(Keep.both).run()

  // Subscriber 1: logs and ignores
  val ignored = broadcastSource
    .log("to be ignored")
    .runWith(Sink.ignore)

  // Subscriber 2: logs and prints to console
  val printed = broadcastSource
    .log("to be printed")
    .runForeach(e => println(s"I received: $e"))

  // Step 3: Emit after both subscribers are attached
  Source(1 to 100).throttle(10, 100.millis).runForeach(queue.offer)
  //Source(1 to 100).runForeach(queue.offer)


  // Wait for completion (only for demo purposes)
  for {
    _ <- ignored
    _ <- printed
  } yield {
    println("All streams completed.")
    system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}