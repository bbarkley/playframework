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
//    println(s"Snapshotting context on ${Thread.currentThread().getName} original is $holder")
//    Option(holder).map{ holder => Map(DataHolderKey -> holder)}
    val copy = Option(holder).map(_.copy())
    println(s"Snapshotting context on ${Thread.currentThread().getName} original is $holder copy is ${copy.orNull}")
    copy.map { holder =>
      Map(DataHolderKey -> holder)
    }
  }


  override def copyContext(context: Map[String, Any]): Map[String, Any] = {
    context.map{
      case (DataHolderKey, v) => (DataHolderKey, v.asInstanceOf[DataHolder].copy())
      case (k, v) => (k, v)
    }
//    Map(DataHolderKey -> context.get(DataHolderKey).map{ dh => dh.asInstanceOf[DataHolder].copy()}.orNull)
  }

  override def wrapRunnable(callSiteCtx: Context, executeCtx: Context, runnable: Runnable): Runnable = {
    new Runnable {
      override def run(): Unit = {
//        val origContext: Map[String, Any] = executeCtx.getOrElse(callSiteCtx.getOrElse(Map()))
        val origContext = callSiteCtx.getOrElse(Map())
        val contextToUse = origContext
//        val origContext: Map[String, Any] = callSiteCtx.getOrElse(Map())
//        val contextToUse = copyContext(origContext)
        println(s"Copying context on ${Thread.currentThread().getName} original is $origContext copy is $contextToUse")
        self.restoreContext(contextToUse)
        try {
          runnable.run()
        } finally {
          val doneDataHolder = self.snapshotContext.flatMap(_.get(DataHolderKey).asInstanceOf[Option[DataHolder]])
          self.clearContext
          origContext.get(DataHolderKey).foreach(_.asInstanceOf[DataHolder].addData(doneDataHolder.map(_.getData).getOrElse(new util.HashMap[String, Object]())))
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