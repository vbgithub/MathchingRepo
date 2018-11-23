package matching

case class ClientOrder(clientName:String, operation:String, stockType:String, price: Int, amount: Int)

case class OrderKey(stockType:String, price:Int, amount:Int)