package dk.betex.server

import scala.collection.JavaConversions._
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory

/**
 * Starts Betex Server.
 *
 * @author korzekwad
 */
case class BetexServer(baseUri: String) {

  private val initParams = Map("com.sun.jersey.config.property.packages" -> "dk.betex.server")

  private val threadSelector =
    GrizzlyWebContainerFactory.create(baseUri, initParams);
  
  def stop() {
    threadSelector.stopEndpoint();
  }

}