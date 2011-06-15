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
import dk.betex.api.IMarket._

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
      resp match {
        case resp: List[IMarket] => {
          val jsonObj = toJSON(resp)
          jsonObj
        }
      }

    } catch {
      case e: Exception => RESPONSE_INPUT_VALIDATION_ERROR + ":" + e.getLocalizedMessage
    }
  }

  @GET
  @Path("/placeBet")
  @Produces(Array("text/plain"))
  def placeBet(@QueryParam("betId") betId: Long, @QueryParam("userId") userId: Int, @QueryParam("betSize") betSize: Double, @QueryParam("betPrice") betPrice: Double,
    @QueryParam("betType") betType: String, @QueryParam("marketId") marketId: Long, @QueryParam("runnerId") runnerId: Long, @QueryParam("placedDate") placedDate: Long): String = {
    try {
      val resp = betexActor !? new PlaceBetEvent(betId, userId, betSize, betPrice, IBet.BetTypeEnum.withName(betType), marketId, runnerId, placedDate)
      resp.toString
    } catch {
      case e: Exception => RESPONSE_INPUT_VALIDATION_ERROR + ":" + e.getLocalizedMessage
    }
  }

  @GET
  @Path("/getBestPrices")
  @Produces(Array("application/json"))
  def getBestPrices(@QueryParam("marketId") marketId: Long): String = {
    try {
      val resp = betexActor !? new GetBestPricesEvent(marketId)

      resp match {
        case resp: String => resp
        case resp: Map[Long, Tuple2[IRunnerPrice, IRunnerPrice]] => {
          val jsonObj = toJSON(resp)
          jsonObj.toString
        }
      }

    } catch {
      case e: Exception => RESPONSE_INPUT_VALIDATION_ERROR + ":" + e.getLocalizedMessage
    }
  }

  @GET
  @Path("/cancelBets")
  @Produces(Array("text/plain"))
  def cancelBets(@QueryParam("userId") userId: Int, @QueryParam("betSize") betSize: Double, @QueryParam("betPrice") betPrice: Double,
    @QueryParam("betType") betType: String, @QueryParam("marketId") marketId: Long, @QueryParam("runnerId") runnerId: Long): String = {
    
     try {
      val resp = betexActor !? new CancelBetsEvent(userId, betSize, betPrice, IBet.BetTypeEnum.withName(betType), marketId, runnerId)
      resp.toString
    } catch {
      case e: Exception => RESPONSE_INPUT_VALIDATION_ERROR + ":" + e.getLocalizedMessage
    }
  }

  /**Key - runnerId, Value - market prices (element 1 - priceToBack, element 2 - priceToLay)*/
  private def toJSON(marketPrices: Map[Long, Tuple2[IRunnerPrice, IRunnerPrice]]): String = {

    val marketPricesObj = new JSONArray()

    for ((runnerId, runnerPrices) <- marketPrices) {
      val runnerPricesObj = new JSONObject()
      runnerPricesObj.put("runnerId", runnerId)
      if (!runnerPrices._1.price.isNaN) {
        runnerPricesObj.put("bestToBackPrice", runnerPrices._1.price)
        runnerPricesObj.put("bestToBackTotal", runnerPrices._1.totalToBack)
      }
      if (!runnerPrices._2.price.isNaN) {
        runnerPricesObj.put("bestToLayPrice", runnerPrices._2.price)
        runnerPricesObj.put("bestToLayTotal", runnerPrices._2.totalToLay)
      }
      marketPricesObj.put(runnerPricesObj)
    }

    val jsonObj = new JSONObject()
    jsonObj.put("marketPrices", marketPricesObj)
    jsonObj.toString()
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