package sideeffect

import java.util.concurrent.{ExecutorService, LinkedBlockingQueue}

import exercises.sideeffect.IOAsyncExercises.{IO, OrderId, User, UserId, UserOrderApi}
import exercises.sideeffect.{IOAsyncExercises, ThreadPoolUtil}
import org.scalatest.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.concurrent.ExecutionContext

class IOAsyncExercisesTest extends AnyFunSuite with Matchers with ScalaCheckDrivenPropertyChecks {

  /////////////////////////
  // 1. Concurrent Program
  /////////////////////////
  class UserOrderApiImpl extends UserOrderApi {
    var results: LinkedBlockingQueue[String] = new LinkedBlockingQueue()
    def deleteOrder(orderId: OrderId): IO[Unit] = {
      IO( {
//        println(orderId)
        results.add(orderId.value)
      })
    }

    override def getUser(userId: IOAsyncExercises.UserId): IO[IOAsyncExercises.User] = {
      IO {
        User(UserId("1234"), "Rob", List(OrderId("1111"), OrderId("5555")))
      }
    }
  }

  test("deleteTwoOrders") {
    {
      val api = new UserOrderApiImpl()
      withExecutionContext(ThreadPoolUtil.fixedSize(4, "deleteTwoOrders")) { ec =>
        for (i <- 0 until 100) {
          IOAsyncExercises.deleteTwoOrders(api, ec)(OrderId("order1"), OrderId("order2")).unsafeRun()
        }
      }
      api.results.toArray().toVector.sliding(2).contains(Vector("order1", "order1")) shouldEqual true
    }
  }

  test("deleteAllUserOrders") {
    val api = new UserOrderApiImpl()
    withExecutionContext(ThreadPoolUtil.fixedSize(4, "deleteTwoOrders")) { ec =>
      for (i <- 0 until 100) {
        IOAsyncExercises.deleteAllUserOrders(api)(UserId("userId")).start(ec).
      }

    }
    println(api.results.toArray().toVector.size)
    api.results.toArray().toVector.sliding(2).contains(Vector("1111", "1111")) shouldEqual true
  }

  def withExecutionContext[A](makeES: => ExecutorService)(f: ExecutionContext => A): A = {
    val es = makeES
    val ec = ExecutionContext.fromExecutorService(es)
    val a  = f(ec)
    es.shutdown()
    a
  }

}
