package com.linkedin.context

import com.linkedin.dataholder.DataHolder
import play.api.libs.concurrent.ContextPropagator

/**
 * @author bbarkley
 */

class DataHolderContextPropagator extends ContextPropagator {
  val DataHolderKey = "data-holder"
  override def snapshotContext: Map[String, Any] = Map(DataHolderKey -> Option(DataHolder.getInstance()).map(_.copy()).orNull)

  override def restoreContext(context: Map[String, Any]): Unit = DataHolder.INSTANCE.set(context.get(DataHolderKey).orNull.asInstanceOf[DataHolder])

  override def clearContext: Unit = DataHolder.INSTANCE.remove()

  override def withInitialContext(context: Map[String, Any]): DataHolderContextPropagator = new DataHolderContextPropagator {
    override def snapshotContext: Map[String, Any] = context
  }
}