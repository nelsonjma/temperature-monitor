package mongodb

import org.mongodb.scala.{Observable, Observer, SingleObservable, Completed, Document}
import org.mongodb.scala.bson.{BsonArray, BsonValue, BsonDouble, BsonInt32, BsonString, BsonBoolean} 

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import org.bson.BsonValue

object Helpers {
  /**
    * Convert BsonValue to other objects
    *
    * @param bsonValue
    */
  implicit class BsonValueTo(bsonValue: org.bson.BsonValue) {
  
    def toInt(key: String): Int = {
      return bsonValue.asDocument().getOrDefault(key, new BsonInt32(0)).asInt32().getValue()
    }

    def toDouble(key: String): Double = {
      return bsonValue.asDocument().getOrDefault(key, new BsonDouble(0.0)).asDouble().getValue()
    }

    def toString(key: String): String = {
      return bsonValue.asDocument().getOrDefault(key, new BsonString("")).asString().getValue()
    }

    def toListBsonValues(key: String): List[BsonValue] = {
      return bsonValue.asDocument().getArray(key).toListBsonValues()
    }
  }

  /**
    * Convert Document to other formats, for now
    * 
    * - Int
    * - List[BsonValue]
    * - Option[BsonValue]
    *
    * @param doc
    */
  implicit class DocumentTo(doc: org.mongodb.scala.Document) {
    def toInt(key: String): Int = {
      return doc.getOrElse[BsonValue](key, 0).asInt32.getValue()
    }

    def toListBsonValues(key: String): List[BsonValue] = {
      try {
        return doc.get[BsonArray](key).get.toListBsonValues()  
      } catch {
        case e: Exception => return List[BsonValue]()
      }
    }

    def toBsonValue(key: String): Option[org.bson.BsonValue] = {
      return doc.get(key)
    }
  }

  /**
    * BsonArray to List[BsonValue]
    *
    * @param ba
    */
  implicit class BsonArrayTo(ba: BsonArray) {
    def toListBsonValues(): List[BsonValue] = {
      val accumulator = new ListBuffer[BsonValue]()
      ba.forEach { bv: BsonValue => if (bv != null) {accumulator.append(bv)} }
      return accumulator.toList
    }
  }

  /** Collect sync and async records from mongodb */
  implicit class ObservableCollect[T](observable: Observable[T]) {
    /** Sync collect 
     * 
     *  @param waitTimeout - by default waits forever :)
     */
    def collectSync(waitTimeout: Duration=Duration.Inf): List[T] = {
      val accumulator = new ListBuffer[T]()
      // convert observable to future
      val observed: Future[Seq[T]] = observable.toFuture   
      // collect response values
      val response = observed.filter(_ != null).map { f => f.map(v => accumulator.append(v)) }
      // wait ...
      Await.ready(response, waitTimeout)

      return accumulator.toList
    }

    /** Async collect 
     * 
     *  @param next - execute on each iteration
     *  @param onEnd - execute in the end, it has a default implementation that does nothing
     *  @param onFail - execute on fail you have access to a throwable
     */
    def collectAsync(next: (T) => Unit, onEnd: () => Unit=() => {}, onFail: (Throwable) => Unit=(t) => {}): Unit = {
      observable.subscribe(new Observer[T]() {
        override def onNext(t: T): Unit = next(t)
        override def onComplete(): Unit = onEnd()
        override def onError(t: Throwable): Unit = onFail(t) // throw new Exception(t.getMessage()) 
      })
    }
  }

  /** Collect sync and async record from mongodb, this is normaly used to collect a response from some operation */
  implicit class ObservableExecute[T](observable: SingleObservable[T]) {
    /** Sync execute 
     * 
     *  Execute operation on mongodb, and waits for response to be completed 
     *   using Wait.ready(...), the response will be inside a Option.
     * 
     *  @param waitTimeout - by default waits forever :)
     */
    def executeSync(waitTimeout: Duration=Duration.Inf): Option[T] = {
      var resp: Option[T] = None
      // convert observable to future
      val observed: Future[T] = observable.toFuture   
      // collect response
      val response = observed.filter(_ != null).map { v => resp = Some(v) }
      // wait ...
      Await.ready(response, waitTimeout)

      return resp
    }

    /** Async execute 
     * 
     *  The execution will be asyncronous, you have to use a callback to do something with the response.
     * 
     *  @param onResponse - execute when response is returned
     *  @param onEnd - execute in the end, it has a default implementation that does nothing
     *  @param onFail - execute on fail you have access to a throwable
     */
    def executeAsync(onResponse: (T) => Unit, onEnd: () => Unit=() => {}, onFail: (Throwable) => Unit=(t) =>{}): Unit = {
      observable.subscribe(new Observer[T]() {
        override def onNext(t: T): Unit = onResponse(t)
        override def onComplete(): Unit = onEnd()
        override def onError(t: Throwable): Unit = onFail(t) // throw new Exception(t.getMessage()) 
      })
    }
  }
}