package app

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import matching.ClientAccountProcessor

import scala.concurrent.ExecutionContext

object AccountProcessingProgram extends App with LazyLogging{

  try {
    // read the config
    val config = ConfigFactory.load("application.conf")
    val clientFilePath = config.getString("app.clientFile")
    val orderFilePath = config.getString("app.orderFile")
    val resultFilePath = config.getString("app.resultFile")

    // implicit variables
    implicit val system: ActorSystem = ActorSystem("AccountProcessingActor")
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val processor = new ClientAccountProcessor()
    // load clients to a map
    val clients = processor.loadClients(clientFilePath)
    // update accounts
    val futTransactionCount = processor.updateClientAccounts(orderFilePath, clients)
    // write results to a file
    futTransactionCount.onComplete(transCount => {
      println(transCount)
      processor.writeToFile(clients, resultFilePath)
      logger.info("Client's accounts are updated. Results are written in results.txt")
      logger.info("Account processing has been finished.")
      system.terminate()
    })
  } catch{
    case exc:Exception =>
      logger.error("Account processing failed.", exc)
  }
}
