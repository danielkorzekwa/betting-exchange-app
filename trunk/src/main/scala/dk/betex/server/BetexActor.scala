package dk.betex.server

import scala.actors._
import dk.betex.api.IMarket
import dk.betex.api.IMarket.IRunner
import java.util.Date
import dk.betex._
import api.IBet.BetTypeEnum._
import org.codehaus.jettison.json._
import dk.betex.eventcollector.eventprocessor._

/**
 * This actor processes all betex requests in a sequence.
 *
 * @author korzekwad
 *
 */
case class BetexActor extends Actor {

  val RESPONSE_OK = "OK"
  val RESPONSE_ERROR = "ERROR"

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
          reply(RESPONSE_OK)
        }

        case GetMarketsEvent => reply(betex.getMarkets())

        case e: GetMarketEvent => reply(betex.findMarket(e.marketId))

        case e: PlaceBetEvent => {
          betex.findMarket(e.marketId).placeBet(nextBetId(), e.userId, e.betSize, e.betPrice, e.betType, e.runnerId, e.placedDate)
          reply(RESPONSE_OK)
        }

        case e: GetBestPricesEvent => reply(betex.findMarket(e.marketId).getBestPrices())

        case e: CancelBetsEvent => {
          betex.findMarket(e.marketId).cancelBets(e.userId, e.betsSize, e.betPrice, e.betType, e.runnerId)
          reply(RESPONSE_OK)
        }

        case e: ProcessMarketEvents => {
          val marketEventsObj = new JSONObject(e.marketEventsJSON)
          val userId = marketEventsObj.get("userId").toString.toInt
          val marketEvents = marketEventsObj.getJSONArray("marketEvents")
          for (i <- 0 until marketEvents.length) {
            val marketEvent = marketEvents.get(i).toString()
            marketEventProcessor.process(marketEvent, nextBetId(), userId)
          }
          reply(RESPONSE_OK)
        }
        case _ => throw new UnsupportedOperationException("Not supported.")
      }
    }
  }

  override def exceptionHandler = {
    case e: Exception => reply(RESPONSE_ERROR + ":" + e.getLocalizedMessage())
  }

}

case class CreateMarketEvent(marketId: Long, marketName: String, eventName: String, numOfWinners: Int, marketTime: Date, runners: List[IRunner])
case object GetMarketsEvent
case class GetMarketEvent(marketId: Long)
case class PlaceBetEvent(userId: Long, betSize: Double, betPrice: Double, betType: BetTypeEnum, marketId: Long, runnerId: Long, placedDate: Long)
case class GetBestPricesEvent(marketId: Long)
case class CancelBetsEvent(userId: Long, betsSize: Double, betPrice: Double, betType: BetTypeEnum, marketId: Long, runnerId: Long)
case class ProcessMarketEvents(marketEventsJSON: String)
