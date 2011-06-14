package dk.betex.server

import scala.collection.JavaConversions._
import javax.ws.rs._
import javax.ws.rs.core.Response
import com.sun.jersey.spi.resource._
import java.util.Date
import dk.betex.Market._
import org.codehaus.jettison.json._
import dk.betex.api._
import IBet.BetTypeEnum._

/**
 * Betex API, provides operations to create market, get markets, place bets, etc.
 *
 * @author korzekwad
 */
@Path("/")
@Singleton
class BetexResource {

  val RESPONSE_INPUT_VALIDATION_ERROR = "INPUT_VALIDATION_ERROR"

  val betexActor = BetexActor()
  betexActor.start()

  @GET
  @Produces(Array("text/plain"))
  def help(): java.io.InputStream = getClass().getClassLoader().getResourceAsStream("help.txt")

  @GET
  @Path("/createMarket")
  @Produces(Array("text/plain"))
  def createMarket(@QueryParam("marketId") marketId: Long,
    @QueryParam("marketName") marketName: String,
    @QueryParam("eventName") eventName: String,
    @QueryParam("numOfWinners") numOfWinners: Int,
    @QueryParam("marketTime") marketTime: Long,
    @QueryParam("runners") runners: String): String = {

    try {
      require(marketId > 0 && numOfWinners > 0 && marketTime > 0)
      val marketRunners = for {
        runner <- runners.split(",")
        val runnerArray = runner.split(":")
      } yield Runner(runnerArray(0).toLong, runnerArray(1))

      val resp = betexActor !? CreateMarketEvent(marketId, marketName, eventName, numOfWinners, new Date(marketTime), marketRunners.toList)
      resp.toString
    } catch {
      case e: Exception => RESPONSE_INPUT_VALIDATION_ERROR + ":" + e.getLocalizedMessage
    }
  }

  @GET
  @Path("/getMarkets")
  @Produces(Array("application/json"))
  def getMarkets(): String = {
    try {
      val resp = betexActor !? GetMarketsEvent
      val jsonObj = toJSON(resp.asInstanceOf[List[IMarket]])
      jsonObj
    } catch {
      case e: Exception => RESPONSE_INPUT_VALIDATION_ERROR + ":" + e.getLocalizedMessage
    }
  }

  @GET
  @Path("/placeBet")
  @Produces(Array("text/plain"))
  def plaeBet(@QueryParam("betId") betId: Long, @QueryParam("userId") userId: Int, @QueryParam("betSize") betSize: Double, @QueryParam("betPrice") betPrice: Double,
    @QueryParam("betType") betType: String, @QueryParam("marketId") marketId: Long, @QueryParam("runnerId") runnerId: Long, @QueryParam("placedDate") placedDate: Long): String = {
    try {
      val betTypeValue = betType match {
        case "BACK" => BACK
        case "LAY" => LAY
      }
      val resp = betexActor !? new PlaceBetEvent(betId, userId, betSize, betPrice, betTypeValue, marketId, runnerId, placedDate)
      resp.toString
    } catch {
      case e: Exception => RESPONSE_INPUT_VALIDATION_ERROR + ":" + e.getLocalizedMessage
    }
  }

  @GET
  @Path("/getBestPrices")
  @Produces(Array("application/json"))
  def getMarkets(@QueryParam("marketId") marketId: Long): String = {
    try {
      val resp = betexActor !? new GetBestPricesEvent(marketId)
      resp.toString
    } catch {
      case e: Exception => RESPONSE_INPUT_VALIDATION_ERROR + ":" + e.getLocalizedMessage
    }
  }

  private def toJSON(markets: List[IMarket]): String = {
    val marketsObj = new JSONArray()
    for (market <- markets) {
      val runnersObj = new JSONArray()
      for (runner <- market.runners) {
        val runnerObj = new JSONObject()
        runnerObj.put("runnerId", runner.runnerId)
        runnerObj.put("runnerName", runner.runnerName)
        runnersObj.put(runnerObj)
      }
      val marketObj = new JSONObject()
      marketObj.put("marketId", market.marketId)
      marketObj.put("marketName", market.marketName)
      marketObj.put("eventName", market.eventName)
      marketObj.put("numOfWinners", market.numOfWinners)
      marketObj.put("marketTime", market.marketTime.getTime())
      marketObj.put("runners", runnersObj)

      marketsObj.put(marketObj)
    }

    val jsonObj = new JSONObject()
    jsonObj.put("markets", marketsObj)
    jsonObj.toString()

  }
}