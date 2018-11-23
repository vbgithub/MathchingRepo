package matching

import java.io.{File, PrintWriter}
import java.nio.file.Paths
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing, Sink}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.HashMap
import scala.concurrent.Future


class ClientAccountProcessor extends LazyLogging {

  val BUY_OPERATION = "B"
  val SELL_OPERATION = "S"

  /***
    * Load client's accounts
    * @param filePath
    * @return
    */
  def loadClients(filePath:String): Map[String, ClientAccount] = {

    val file = new java.io.File(filePath)
    if(!file.exists()) {
      logger.error(String.format("File %s does not exist", filePath))
      throw new Exception("The file with clients data does not exists")
    }
    // read lines from the file and parse them
    val clients = scala.io.Source.fromFile(filePath).getLines
      .map(line => {
      val clientData = line.split('\t')
      val clientAccount = DataLoader.loadClientAccount(clientData)
      (clientAccount.clientName, clientAccount)
    }).toMap
    logger.info(String.format("Clients are loaded from %s", filePath))
    // return value
    clients
  }

  /**
    * execute orders and update client's accounts
    * @param filePath
    * @param clients
    * @param system
    * @param materializer
    * @return
    */
  def updateClientAccounts(filePath:String, clients:Map[String, ClientAccount])
                          (implicit system:ActorSystem, materializer:ActorMaterializer): Future[Int] = {
    // check if a file exists
    val file = new java.io.File(filePath)
    if(!file.exists()) {
      throw new Exception("The file with orders data does not exists")
    }

    // in the end, count the number of successful transactions
    val sink: Sink[Int, Future[Int]] = Sink.fold(0)(_ + _)

    val buyRequest: HashMap[OrderKey, String] = HashMap()
    val sellRequest: HashMap[OrderKey, String] = HashMap()

    val futTransactionCount:Future[Int] = FileIO.fromPath(file.toPath)
      .via(Framing.delimiter(ByteString("\n"), 256, true).map(_.utf8String))
      .map(line => {
        val dataAr = line.split('\t')
        val order = DataLoader.loadClientOrder(dataAr)
        order
      }).map(order => {
      var isTransactionDone = false
      val clientName:String = order.clientName
      if(clients.contains(clientName)) {
        val orderKey = OrderKey(order.stockType, order.price, order.amount)
        order.operation.toUpperCase match {
          case SELL_OPERATION =>
            if (buyRequest.contains(orderKey)) {
              val buyingClientName = buyRequest.get(orderKey).get
              val sellingClientName = clientName
              if(buyingClientName != sellingClientName)
                executeTransaction(clients, sellingClientName, buyingClientName, orderKey)
              isTransactionDone = true
            } else {
              sellRequest += (orderKey -> clientName)
            }
          case BUY_OPERATION =>
            if (sellRequest.contains(orderKey)) {
              val sellingClientName = sellRequest.get(orderKey).get
              val buyingClientName = clientName
              if(buyingClientName != sellingClientName)
                executeTransaction(clients, sellingClientName, buyingClientName, orderKey)
              isTransactionDone = true
            } else {
              buyRequest += (orderKey -> clientName)
            }
        }
      }
      if(isTransactionDone)
        1
      else
        0
    }).runWith(sink)

    futTransactionCount
  }

  /**
    * write results to a file
    * @param clients
    * @param filePath
    */
  def writeToFile(clients:Map[String, ClientAccount], filePath:String): Unit = {
   val writer = new PrintWriter(new File(filePath))

   val clientNameList = clients.keySet.toSeq.sorted
   clientNameList.map(name => {
     val clientAccount = clients.get(name).get
     writer.write(clientAccount.toString)
   })

   writer.close()
 }

 /**
   * perform a transaction between two accounts
   * @param clients
   * @param sellingClient
   * @param buyingClient
   * @param orderInfo
   * @return
   */
 def executeTransaction(clients:Map[String, ClientAccount], sellingClient:String, buyingClient:String,
                   orderInfo:OrderKey): Unit = {
   try {
     if (!clients.contains(sellingClient)) {
       logger.warn(String.format("Client %s does not exist", sellingClient))
     }
     if (!clients.contains(buyingClient)) {
       logger.warn(String.format("Client %s does not exist", buyingClient))
     }

     val sellingClientAccount: ClientAccount = clients.get(sellingClient).get
     val buyingClientAccount: ClientAccount = clients.get(buyingClient).get
     sellingClientAccount.sellStock(orderInfo.stockType, orderInfo.price, orderInfo.amount)
     buyingClientAccount.buyStock(orderInfo.stockType, orderInfo.price, orderInfo.amount)
   } catch {
     case exc:Exception =>
       logger.error("Error during the transaction", exc)
   }
 }
}
