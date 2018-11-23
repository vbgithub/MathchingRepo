package matching

/*
  loads information of clients and orders
 */
object DataLoader {

  def loadClientAccount(dataAr:Array[String]): ClientAccount = {
    new ClientAccount(dataAr(0), dataAr(1).toInt, dataAr(2)toInt, dataAr(3).toInt, dataAr(4).toInt, dataAr(5).toInt)
  }

  def loadClientOrder(dataAr:Array[String]): ClientOrder = {
    ClientOrder(dataAr(0), dataAr(1), dataAr(2), dataAr(3).toInt, dataAr(4).toInt)
  }
}
