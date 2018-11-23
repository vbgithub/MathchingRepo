package matching


class ClientAccount(var clientName:String, var moneyBalance: Int, var aStock: Int, var bStock: Int, var cStock: Int, var dStock: Int) {

  def buyStock(stockType:String, price:Int, amount:Int): Unit = {
    stockType.toUpperCase match {
      case "A" => aStock += amount
      case "B" => bStock += amount
      case "C" => cStock += amount
      case "D" => dStock += amount
    }
    moneyBalance -= price*amount
  }

  def sellStock(stockType:String, price:Int, amount:Int): Unit = {
    stockType.toUpperCase match {
      case "A" => aStock -= amount
      case "B" => bStock -= amount
      case "C" => cStock -= amount
      case "D" => dStock -= amount
    }
    moneyBalance += price*amount
  }

  override def toString: String = {
    val elements = Array(clientName, moneyBalance, aStock, bStock, cStock, dStock)
    val str = elements.mkString("\t") + "\n"
    str
  }
}