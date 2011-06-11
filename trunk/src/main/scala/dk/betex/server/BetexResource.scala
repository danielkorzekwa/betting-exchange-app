package dk.betex.server

import javax.ws.rs._
import javax.ws.rs.core.Response

/**Betex API, provides operations to create market, get markets, place bets, etc.
 * 
 * @author korzekwad
 */
@Path("/")
class BetexResource {

  @GET
  @Produces(Array("text/plain"))
  def help(): String = {
    "Help."
  }

  @GET
  @Path("/createMarket")
  @Produces(Array("text/plain"))
  def createMarket(@QueryParam("marketId") marketId: String): String = {
    "Not implemented yet.MarketId=" + marketId
  }
  
  @GET
  @Path("/getMarkets")
  @Produces(Array("text/plain"))
  def getMarkets(): String = {
    "Not implemented yet."
  }
}