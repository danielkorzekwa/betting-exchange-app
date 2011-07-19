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
import BetexActor._

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
  @Produces(Array("application/json"))
  def createMarket(@QueryParam("marketId") marketId: Long,
    @QueryParam("marketName") marketName: String,
    @QueryParam("eventName") eventName: String,
    @QueryParam("numOfWinners") numOfWinners: Int,
    @QueryParam("marketTime") marketTime: Long,
    @QueryParam("runners") runners: String): String = {

    process {
      require(marketId > 0 && numOfWinners > 0 && marketTime > 0)
      val marketRunners = for {
        runner <- runners.split(",")
        val runnerArray = runner.split(":")
      } yield Runner(runnerArray(0).toLong, runnerArray(1))

      CreateMarketEvent(marketId, marketName, eventName, numOfWinners, new Date(marketTime), marketRunners.toList)
    }
  }

  @GET
  @Path("/getMarkets")
  @Produces(Array("application/json"))
  def getMarkets(): String = {
    process { GetMarketsEvent }
  }

  @GET
  @Path("/getMarket")
  @Produces(Array("application/json"))
  def getMarket(@QueryParam("marketId") marketId: Long): String = {
    process { GetMarketEvent(marketId) }
  }

  @GET
  @Path("/removeMarket")
  @Produces(Array("application/json"))
  def removeBet(@QueryParam("marketId") marketId: Long): String = {
    process { RemoveMarketEvent(marketId) }

  }

  @GET
  @Path("/placeBet")
  @Produces(Array("application/json"))
  def placeBet(@QueryParam("userId") userId: Int, @QueryParam("betSize") betSize: Double, @QueryParam("betPrice") betPrice: Double,
    @QueryParam("betType") betType: String, @QueryParam("marketId") marketId: Long, @QueryParam("runnerId") runnerId: Long, @QueryParam("placedDate") placedDate: Long): String = {
    process {

      val betTypeValue = try {
        IBet.BetTypeEnum.withName(betType)
      } catch {
        case e: Exception => throw new RuntimeException("Incorrect betType = %s. Supported values = %s".format(betType, IBet.BetTypeEnum.toString()))
      }

      PlaceBetEvent(userId, betSize, betPrice, betTypeValue, marketId, runnerId, placedDate)
    }
  }

  @GET
  @Path("/getBestPrices")
  @Produces(Array("application/json"))
  def getBestPrices(@QueryParam("marketId") marketId: Long): String = {
    process { GetBestPricesEvent(marketId) }

  }

  @GET
  @Path("/cancelBets")
  @Produces(Array("application/json"))
  def cancelBets(@QueryParam("userId") userId: Int, @QueryParam("betSize") betSize: Double, @QueryParam("betPrice") betPrice: Double,
    @QueryParam("betType") betType: String, @QueryParam("marketId") marketId: Long, @QueryParam("runnerId") runnerId: Long): String = {
    process { new CancelBetsEvent(userId, betSize, betPrice, IBet.BetTypeEnum.withName(betType), marketId, runnerId) }
  }

  @GET
  @Path("/getMarketProbability")
  @Produces(Array("application/json"))
  def getMarketProbability(@QueryParam("marketId") marketId: Long, @QueryParam("probType") probType: String): String = {

    process {
      val probTypeValue =
        if (probType == null) None
        else try {
          Option(MarketProbTypeEnum.withName(probType))
        } catch {
          case e: Exception => throw new RuntimeException("Incorrect probType = %s. Supported values = %s".format(probType, MarketProbTypeEnum.toString()))
        }

      GetMarketProbEvent(marketId, probTypeValue)
    }
  }

  @GET
  @Path("/getRisk")
  @Produces(Array("application/json"))
  def getRisk(@QueryParam("userId") userId: Int, @QueryParam("marketId") marketId: Long): String = {
    process {
      require(userId > 0, "User id parameter not found.")
      require(marketId > 0, "Market id parameter not found.")
      new GetRiskEvent(userId, marketId)
    }
  }

  @GET
  @Path("/hedge")
  @Produces(Array("application/json"))
  def hedge(@QueryParam("userId") userId: Int, @QueryParam("marketId") marketId: Long, @QueryParam("simulate") simulate: Boolean): String = {
    process {
      require(userId > 0, "User id parameter not found.")
      require(marketId > 0, "Market id parameter not found.")
      new HedgeEvent(userId,marketId,simulate)
    }
  }
  
  @POST
  @Path("/processBetexEvents")
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def processBetexEvents(message: String): String = {
    process {
      new ProcessMarketEvents(message)
    }
  }

  private def process(betexEvent: => Any): String = {
    try {
      val betexResponse = (betexActor !? betexEvent).asInstanceOf[BetexResponseEvent]
      betexResponse.status match {
        case RESPONSE_OK =>
          betexResponse.data match {
            case Some(e) => BetexJSONParser.toJSON(e).toString()
            case None => toJSONStatus(betexResponse.status).toString()
          }
        case _ => toJSONStatus(betexResponse.status).toString()

      }

    } catch {
      case e: Exception => toJSONStatus(RESPONSE_INPUT_VALIDATION_ERROR + ":" + e.getLocalizedMessage).toString()
    }
  }

  private def toJSONStatus(status: String): JSONObject = {
    val obj = new JSONObject()
    obj.put("status", status)
    obj
  }

}