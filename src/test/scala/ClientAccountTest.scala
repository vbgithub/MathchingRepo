import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import app.AccountProcessingProgram.logger
import matching.{ClientAccountProcessor, DataLoader, OrderKey}
import org.scalatest._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try

class ClientAccountTest extends FlatSpec with Matchers {

  val clientFilePath = getClass.getResource("/fakeClients.txt").getPath
  val orderFilePath = getClass.getResource("/fakeOrders.txt").getPath

  "Client's row" should "be correctly parsed" in {
    val row = "C1\t1000\t130\t240\t760\t320"
    val client = DataLoader.loadClientAccount(row.split("\t"))

    client.clientName should be ("C1")
    client.moneyBalance should be (1000)
    client.aStock should be (130)
    client.bStock should be (240)
    client.cStock should be (760)
    client.dStock should be (320)
  }

  "Order's row" should "be correctly parsed" in {
    val row = "C1\ts\tB\t6\t1"
    val client = DataLoader.loadClientOrder(row.split("\t"))

    client.clientName should be ("C1")
    client.operation should be ("s")
    client.stockType should be ("B")
    client.price should be (6)
    client.amount should be (1)
  }

  // check if a missing fakeClients.txt file leads to an exception
  the [Exception] thrownBy {
    val processor = new ClientAccountProcessor()
    // load clients to a map
    val clients = processor.loadClients("data_files/wrongName.txt")
  }

  val NUMBER_OF_CLIENTS = 2
  "The number of read clients" should s"equals to $NUMBER_OF_CLIENTS" in {
    val processor = new ClientAccountProcessor()
    val clients = processor.loadClients(clientFilePath)
    clients.size should be (NUMBER_OF_CLIENTS)
  }

  "Transaction between two accounts" should "lead to correct balance" in {
    val processor = new ClientAccountProcessor()
    val clients = processor.loadClients(clientFilePath)

    val c1MoneyBalance = clients.get("C1").get.moneyBalance
    val c2MoneyBalance = clients.get("C2").get.moneyBalance
    val c1AStock = clients.get("C1").get.aStock
    val c2AStock = clients.get("C2").get.aStock
    processor.executeTransaction(clients, "C1", "C2", OrderKey("A", 5, 2))

    clients.get("C1").get.moneyBalance should be (c1MoneyBalance+10)
    clients.get("C2").get.moneyBalance should be (c2MoneyBalance-10)
    clients.get("C1").get.aStock should be (c1AStock-2)
    clients.get("C2").get.aStock should be (c2AStock+2)
  }

  val MONEY_AMOUNT = 1150
  val AStock_NUMBER = 15
  val BStock_NUMBER = 15
  val CStock_NUMBER = 15
  val DStock_NUMBER = 15
  "The amount of money and stocks for all clients" should s"be correct" in {
    val processor = new ClientAccountProcessor()
    val clients = processor.loadClients(clientFilePath)
    val money = clients.valuesIterator.toList.map(account => account.moneyBalance).sum
    val aStock = clients.valuesIterator.toList.map(account => account.aStock).sum
    val bStock = clients.valuesIterator.toList.map(account => account.bStock).sum
    val cStock = clients.valuesIterator.toList.map(account => account.cStock).sum
    val dStock = clients.valuesIterator.toList.map(account => account.dStock).sum

    money should be (MONEY_AMOUNT)
    aStock should be (AStock_NUMBER)
    bStock should be (BStock_NUMBER)
    cStock should be (CStock_NUMBER)
    dStock should be (DStock_NUMBER)
  }

  "The amount of money and stocks after executing orders" should s"be as before" in {
    val processor = new ClientAccountProcessor()
    val clients = processor.loadClients("data_files/results.txt")
    val money = clients.valuesIterator.toList.map(account => account.moneyBalance).sum
    val aStock = clients.valuesIterator.toList.map(account => account.aStock).sum
    val bStock = clients.valuesIterator.toList.map(account => account.bStock).sum
    val cStock = clients.valuesIterator.toList.map(account => account.cStock).sum
    val dStock = clients.valuesIterator.toList.map(account => account.dStock).sum

    money should be (MONEY_AMOUNT)
    aStock should be (AStock_NUMBER)
    bStock should be (BStock_NUMBER)
    cStock should be (CStock_NUMBER)
    dStock should be (DStock_NUMBER)
  }

  "The full test on smaller set of data" should "pass" in {
    // implicit variables
    implicit val system: ActorSystem = ActorSystem("AccountProcessingActor")
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val processor = new ClientAccountProcessor()
    // load clients to a map
    val clients = processor.loadClients(clientFilePath)

    // memorise money and the number of C stocks
    val C1_MONEY = clients.get("C1").get.moneyBalance
    val C2_MONEY = clients.get("C2").get.moneyBalance
    val C1_CSTOCK = clients.get("C1").get.cStock
    val C2_CSTOCK = clients.get("C2").get.cStock

    // update accounts
    val futTransactionCount = processor.updateClientAccounts(orderFilePath, clients)
    val result: Try[Int] = Await.ready(futTransactionCount, Duration.Inf).value.get
    system.terminate()

    clients.get("C1").get.moneyBalance should be (C1_MONEY + 50)
    clients.get("C2").get.moneyBalance should be (C2_MONEY - 50)
    clients.get("C1").get.cStock should be (C1_CSTOCK - 5)
    clients.get("C2").get.cStock should be (C2_CSTOCK + 5)
  }
}
