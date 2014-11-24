package com.twitter.finagle.httpx

import com.twitter.finagle.transport.Transport
import com.twitter.finagle.httpx.codec.ConnectionManager
import com.twitter.util.{Future, Time}
import scala.util.control.NonFatal

/**
 * A Transport with close behavior managed by ConnectionManager.
 */
class HttpTransport(self: Transport[Any, Any], manager: ConnectionManager)
  extends Transport[Any, Any] {

  def this(self: Transport[Any, Any]) = this(self, new ConnectionManager)

  def close(deadline: Time) = self.close(deadline)

  def read(): Future[Any] =
    self.read() onSuccess { m =>
      manager.observeMessage(m)
      if (manager.shouldClose)
        self.close()
    }

  def write(m: Any): Future[Unit] =
    try {
      manager.observeMessage(m)
      val f = self.write(m)
      if (manager.shouldClose) f before self.close()
      else f
    } catch {
      case NonFatal(e) => Future.exception(e)
    }

  def isOpen = !manager.shouldClose && self.isOpen

  def localAddress = self.localAddress

  def remoteAddress = self.remoteAddress

  val onClose = self.onClose
}
