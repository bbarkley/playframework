package controllers

import com.linkedin.dataholder.DataHolder
import play.api.data.Forms._
import play.api.data._
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import views._
import scala.concurrent.duration._
import scala.concurrent._

object Application extends Controller {

  /**
   * Describes the hello form.
   */
  val helloForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "repeat" -> number(min = 1, max = 100),
      "color" -> optional(text)
    )
  )

  // -- Actions

  /**
   * Home page
   */
  def index = Action.async {
    val dataHolder = new DataHolder
    DataHolder.INSTANCE.set(dataHolder)
    setCount(2)
    println("after initial set should be 2 " + buildCount)

    val taRequest = WS.url("http://www.tripadvisor.com").get.map{ response =>
      println("In second WS map " + buildCount)
      Thread.sleep(700);
      println("Done waiting in second WS map " + buildCount)
      setCount(4)
      println("Set in second WS map should be 4" + buildCount)
      response
    }

    val liRequest = WS.url("http://www.linkedin.com").get.map{ response =>
      println("In first WS map " + buildCount)
      Thread.sleep(300);
      println("Done waiting in first WS map " + buildCount)
      setCount(3)
      println("Set in first WS map should be 3 " + buildCount)
      response
    }
    val other = Future{
      println("waiting in other future " + buildCount)
      Thread.sleep(500)
      println("done waiting in other future " + buildCount)
      setCount(5)
      println("set in other future should have 5" + buildCount)
      "FutureString!"
    }.map{ str =>
      setCount(6) // 6
      println("set in other's map and should have 6 " + buildCount)
      str
    }

    other.onComplete { result =>
      println("In oncomplete 1 about to increment " + buildCount)
      Thread.sleep(300)
      setCount(7)
      println("In oncomplete 1 after set should be 7 and is " + buildCount)
    }

    other.onComplete { result =>
      println("In oncomplete 2 about to increment " + buildCount)
      Thread.sleep(500)
      setCount(8)
      println("In oncomplete 2 after set should be 8 and is" + buildCount)
    }
    val ret = for {
      trip <- taRequest
      lnkd <- liRequest
      o <- other
    } yield {
      // XXX - weird behavior here - for-comprehensions are implemented as chained flatmaps
      // XXX - so we'll actually get the result of the future that is last in the chain that has a map applied on it
      // XXX - flipping the order of trip and lnkd gives different results
      // XXX - not intuitive but I don't think we can get around this
      println("finally done and is " + buildCount)
      Ok(s"LI: ${lnkd.status} and TA ${trip.status} $o")
    }

    println("Outside of for block and is " + buildCount)
    Await.result(ret, 10 seconds)
    println("After await and is " + buildCount)
    ret

  }

  /**
   * Handles the form submission.
   */
  def sayHello = Action { implicit request =>
    helloForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.index(formWithErrors)),
      {case (name, repeat, color) => Ok(html.hello(name, repeat.toInt, color))}
    )
  }

  def setCount(count: Int) = Option(DataHolder.getInstance()).foreach { data =>
    println(s"setting to $count for " + System.identityHashCode(data) + " on thread " + Thread.currentThread().getName)
    data.setCount(count)
  }
  def buildCount = {
    val dataOpt = Option(DataHolder.getInstance())
    s"---->Count is ${dataOpt.map(_.getCount).orNull} for ${dataOpt.map(System.identityHashCode(_)).orNull} on thread ${Thread.currentThread().getName}"
  }
}
