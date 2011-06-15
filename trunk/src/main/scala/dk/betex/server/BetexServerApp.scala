package dk.betex.server

import scala.collection.JavaConversions._
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory

/**
 * The main class for Betex Server application.
 *
 * @author korzekwad
 */
object BetexServerApp extends App {
  val port = if (args.isEmpty) 80 else args(0).toInt
  
  val baseUri = "http://localhost:%s/".format(port);
  val initParams = Map("com.sun.jersey.config.property.packages" -> "dk.betex.server")

  printHeader()
  
  println("Starting Betex Server...");
  val threadSelector =
  GrizzlyWebContainerFactory.create(baseUri, initParams);
  println("Betex Server started with WADL available at %sapplication.wadl\nTry out %s\nHit enter to stop it...".format(baseUri, baseUri));
  System.in.read();
  threadSelector.stopEndpoint();
  System.exit(0);
  
   private def printHeader() {
    println("")
    println("***********************************************************************************")
    println("*Betting Exchange Server Copyright 2011 Daniel Korzekwa(http://danmachine.com)    *")
    println("*Project homepage: http://code.google.com/p/betting-exchange-app/                 *")
    println("*Licenced under Apache License 2.0(http://www.apache.org/licenses/LICENSE-2.0)    *")
    println("***********************************************************************************")
    println("")
  } 
}