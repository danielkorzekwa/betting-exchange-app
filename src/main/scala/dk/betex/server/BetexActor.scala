package dk.betex.server

import scala.actors._
import dk.betex.api.IMarket
import dk.betex.api.IMarket.IRunner
import java.util.Date
import dk.betex._
import api.IBet.BetTypeEnum._
import org.codehaus.jettison.json._
import dk.betex.eventcollector.eventprocessor._
import scala.collection.immutable.ListMap
import dk.betex.api.IMarket._
import BetexActor.MarketProbTypeEnum.MarketProbTypeEnum
import dk.bettingai.marketsimulator.risk._
import scala.collection._
import dk.bettingai.risk.prob._

/**
 * This actor processes all betex requests in a sequence.
 *
 * @author korzekwad
 *
 */

object BetexActor {

  val RESPONSE_OK = "OK"
  val RESPONSE_ERROR = "ERROR"

  case class CreateMarketEvent(marketId: Long, marketName: String, eventName: String, numOfWinners: Int, marketTime: Date, runners: List[IRunner])
  case object GetMarketsEvent
  case class GetMarketEvent(marketId: Long)
  case class RemoveMarketEvent(marketId: Long)
  case class PlaceBetEvent(userId: Long, betSize: Double, betPrice: Double, betType: BetTypeEnum, marketId: Long, runnerId: Long, placedDate: Long)
  case class GetBestPricesEvent(marketId: Long)
  case class CancelBetsEvent(userId: Long, betsSize: Double, betPrice: Double, betType: BetTypeEnum, marketId: Long, runnerId: Long)
  case class ProcessMarketEvents(marketEventsJSON: String)

  case class GetMarketProbEvent(marketId: Long, marketProbType: MarketProbTypeEnum)

  case class BetexResponseEvent(status: String, data: Option[Any] = None)

  object MarketProbTypeEnum extends Enumeration {
    type MarketProbTypeEnum = Value
    val WIN = Value("WIN") // winner market
    val PLACE = Value("PLACE") //two winner market
    val SHOW = Value("SHOW") //three winner market

    override def toString() = MarketProbTypeEnum.values.mkString("MarketProbTypeEnum [", ", ", "]")
  }

  case class MarketProb(marketId: Long, probType: MarketProbTypeEnum, probs: Map[Long, Double])

}

case class BetexActor extends Actor {

  import BetexActor._
  import MarketProbTypeEnum._

  private val betex = new Betex()
  private var lastBetId: Long = 1
  private val marketEventProcessor = new MarketEventProcessorImpl(betex)

  private def nextBetId(): Long = {
    lastBetId += 1
    lastBetId
  }

  def act() {
    loop {
      react {
        case e: CreateMarketEvent => {

          betex.createMarket(e.marketId, e.marketName, e.eventName, e.numOfWinners, e.marketTime, e.runners)
          reply(BetexResponseEvent(RESPONSE_OK))
        }

        case GetMarketsEvent => reply(BetexResponseEvent(RESPONSE_OK, Option(betex.getMarkets())))

        case e: GetMarketEvent => reply(BetexResponseEvent(RESPONSE_OK, Option(betex.findMarket(e.marketId))))

        case e: RemoveMarketEvent => {
          betex.removeMarket(e.marketId)
          reply(BetexResponseEvent(RESPONSE_OK))
        }

        case e: PlaceBetEvent => {
          betex.findMarket(e.marketId).placeBet(nextBetId(), e.userId, e.betSize, e.betPrice, e.betType, e.runnerId, e.placedDate)
          reply(BetexResponseEvent(RESPONSE_OK))
        }

        case e: GetBestPricesEvent => {
          val market = betex.findMarket(e.marketId)
          val bestPrices = market.getBestPrices()
          /**return runner prices in the same order as market runners.*/
          val orderedPrices = for (runner <- market.runners) yield (runner.runnerId, bestPrices(runner.runnerId))
          val orderedPricesMap = ListMap(orderedPrices: _*)
          reply(BetexResponseEvent(RESPONSE_OK, Option(orderedPricesMap)))
        }

        case e: CancelBetsEvent => {
          betex.findMarket(e.marketId).cancelBets(e.userId, e.betsSize, e.betPrice, e.betType, e.runnerId)
          reply(BetexResponseEvent(RESPONSE_OK))
        }

        case e: GetMarketProbEvent => {
          val market = betex.findMarket(e.marketId)
          val bestPrices = market.getBestPrices.mapValues(prices => prices._1.price -> prices._2.price)
          val marketProbValue = ProbabilityCalculator.calculate(bestPrices, market.numOfWinners)

          val marketProb = market.numOfWinners match {
            case 1 if e.marketProbType == WIN => MarketProb(e.marketId, e.marketProbType, marketProbValue)

            case 1 if e.marketProbType == PLACE => {
              /**Map win probabilities to place probabilities.*/
              val placeMarketProb = marketProbValue.map { case (runnerId, prob) => runnerId -> OrderingProb.calcPlaceProb(runnerId, marketProbValue) }
              MarketProb(e.marketId, e.marketProbType, placeMarketProb)
            }

            case 1 if e.marketProbType == SHOW => {
              /**Map win probabilities to place probabilities.*/
              val showMarketProb = marketProbValue.map { case (runnerId, prob) => runnerId -> OrderingProb.calcShowProb(runnerId, marketProbValue) }
              MarketProb(e.marketId, e.marketProbType, showMarketProb)
            }

            case 2 if e.marketProbType == PLACE => MarketProb(e.marketId, e.marketProbType, marketProbValue)

            case x: Int if x >= 3 && e.marketProbType == SHOW => MarketProb(e.marketId, e.marketProbType, marketProbValue)

            case _ => throw new UnsupportedOperationException("Can't calculate probability for numOfWinners=%s and probType=%s.".format(market.numOfWinners, e.marketProbType))
          }

          reply(BetexResponseEvent(RESPONSE_OK, Option(marketProb)))
        }

        case e: ProcessMarketEvents => {
          val marketEventsObj = new JSONObject(e.marketEventsJSON)
          val userId = marketEventsObj.get("userId").toString.toInt
          val marketEvents = marketEventsObj.getJSONArray("marketEvents")
          for (i <- 0 until marketEvents.length) {
            val marketEvent = marketEvents.get(i).toString()
            marketEventProcessor.process(marketEvent, nextBetId(), userId)
          }
          reply(BetexResponseEvent(RESPONSE_OK))
        }
        case _ => throw new UnsupportedOperationException("Not supported.")
      }
    }
  }

  override def exceptionHandler = {
    case e: Exception => {
      val errorResponse = BetexResponseEvent(RESPONSE_ERROR + ":" + e.getLocalizedMessage())
      reply(errorResponse)
    }
  }

}