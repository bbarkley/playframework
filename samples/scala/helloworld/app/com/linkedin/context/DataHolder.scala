package com.linkedin.context

import java.util

import com.linkedin.dataholder.DataHolder
import play.api.libs.concurrent.ContextPropagator

/**
 * @author bbarkley
 */

class DataHolderContextPropagator extends ContextPropagator { self =>
  val DataHolderKey = "data-holder"
  override def snapshotContext: Option[Map[String, Any]] = {
    val holder = DataHolder.getInstance()
    val copy = Option(holder).map(_.copy())
    println(s"Snapshotting context on ${Thread.currentThread().getName} original is $holder copy is ${copy.orNull}")
    copy.map { holder =>
      Map(DataHolderKey -> holder)
    }
  }


  override def wrapRunnable(callSiteCtx: Context, executeCtx: Context, runnable: Runnable): Runnable = {
    new Runnable {
      override def run(): Unit = {
        val contextToUse: Map[String, Any] = executeCtx.getOrElse(callSiteCtx.getOrElse(Map()))
        self.restoreContext(contextToUse)
        try {
          runnable.run()
        } finally {
          val doneDataHolder = self.snapshotContext.flatMap(_.get(DataHolderKey).asInstanceOf[Option[DataHolder]])
          self.clearContext
          contextToUse.get(DataHolderKey).foreach(_.asInstanceOf[DataHolder].addData(doneDataHolder.map(_.getData).getOrElse(new util.HashMap[String, Object]())))
        }
      }
    }
  }

  override def restoreContext(context: Map[String, Any]): Unit = DataHolder.INSTANCE.set(context.get(DataHolderKey).orNull.asInstanceOf[DataHolder])

  override def clearContext: Unit = DataHolder.INSTANCE.remove()

  override def withInitialContext(context: Map[String, Any]): DataHolderContextPropagator = new DataHolderContextPropagator {
    override def snapshotContext: Option[Map[String, Any]] = Option(context)
  }
}