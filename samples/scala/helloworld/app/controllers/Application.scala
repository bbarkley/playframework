package controllers

import com.linkedin.dataholder.DataHolder
import play.api.data.Forms._
import play.api.data._
import play.api.libs.concurrent.ThreadLogBuffer
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
    setCount(22)
    addData(0)
    log("after initial set should be 22 " + buildCount)

    val taRequest = WS.url("http://www.tripadvisor.com").get.map{ response =>
      log("In second WS map " + buildCount)
      Thread.sleep(700);
      log("Done waiting in second WS map should be 22 " + buildCount)
      setCount(44)
      addData(44)
      log("Set in second WS map should be 44" + buildCount)
      response
    }

    val liRequest = WS.url("http://www.linkedin.com").get.map{ response =>
      log("In first WS map " + buildCount)
      Thread.sleep(300);
      log("Done waiting in first WS map should be 22 " + buildCount)
      setCount(33)
      addData(33)
      log("Set in first WS map should be 33 " + buildCount)
      response
    }
    val other = Future{
      log("waiting in other future should be 22 " + buildCount)
      Thread.sleep(100)
      log("done waiting in other future should be 22 " + buildCount)
      setCount(55)
      addData(55)
      log("set in other future should have 55" + buildCount)
      "FutureString!"
    }.map{ str =>
      log("before set in other's map and should have 55 " + buildCount)
      setCount(66)
      addData(66)
      log("set in other's map and should have 66 " + buildCount)
      str
    }

    other.onComplete { result =>
      log("In oncomplete 1 about should have 66 " + buildCount)
      Thread.sleep(1300)
      setCount(77)
      addData(77)
      log("In oncomplete 1 after set should be 77 and is " + buildCount)
    }

    other.onComplete { result =>
      log("In oncomplete 2 about should have 66 " + buildCount)
      Thread.sleep(500)
      setCount(88)
      addData(88)
      log("In oncomplete 2 after set should be 88 and is" + buildCount)
    }
    val retRaw = taRequest.flatMap{ taResult =>
      liRequest.flatMap { liResult =>
        other.map { otherResult =>
          // would expect all data to be visible here
          log("finally done in flatmap and is " + buildCount)
          Ok(s"LI: ${liResult.status} and TA ${taResult.status} $otherResult")
        }
      }
    }

//    val ret = retRaw
    val ret: Future[SimpleResult] = for {
      trip <- taRequest
      o <- other
      lnkd <- liRequest
    } yield {
      // XXX - weird behavior here - for-comprehensions are implemented as chained flatmaps
      // XXX - but we seem to get the result from the execution that takes the longest to complete
      // XXX - not sure what can be done to get around this
      log("finally done and is " + buildCount)
      Ok(s"LI: ${lnkd.status} and TA ${trip.status} $o")
    }

    log("Outside of for block should be 22 " + buildCount)
    Await.result(ret, 10 seconds)
    log("After await should be 22 " + buildCount)
    ret

  }

  def log(s: String) = {
    println(s)
    ThreadLogBuffer.log(s + "\n")
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

  def addData(count: Int) = Option(DataHolder.getInstance()).foreach { data =>
    data.addData(count.toString, count)
  }

  def setCount(count: Int) = Option(DataHolder.getInstance()).foreach { data =>
    log(s"setting to $count for " + System.identityHashCode(data) + " on thread " + Thread.currentThread().getName)
    data.setCount(count)
  }
  def buildCount = {
    val dataOpt = Option(DataHolder.getInstance())
    s"---->Count is ${dataOpt.map(_.getCount).orNull} data is ${dataOpt.map(_.getData)} for ${dataOpt.map(System.identityHashCode(_)).orNull} on thread ${Thread.currentThread().getName}"
  }
}
