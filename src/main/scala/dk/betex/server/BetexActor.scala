package dk.betex.server

import scala.actors._
import dk.betex.api.IMarket
import dk.betex.api.IMarket.IRunner
import java.util.Date
import dk.betex._
import api.IBet.BetTypeEnum._

/**
 * This actor processes all betex requests in a sequence.
 *
 * @author korzekwad
 *
 */
case class BetexActor extends Actor {

  val RESPONSE_OK = "OK"
  val RESPONSE_ERROR = "ERROR"

  val betex = new Betex()

  def act() {
    loop {
      react {
        case e: CreateMarketEvent => {
          betex.createMarket(e.marketId, e.marketName, e.eventName, e.numOfWinners, e.marketTime, e.runners)
          reply(RESPONSE_OK)
        }
        
        case GetMarketsEvent => reply(betex.getMarkets())
        
        case e: PlaceBetEvent => {
          betex.findMarket(e.marketId).placeBet(e.betId, e.userId, e.betSize, e.betPrice, e.betType, e.runnerId, e.placedDate)
          reply(RESPONSE_OK)
        }
        
        case e:GetBestPricesEvent => reply(betex.findMarket(e.marketId).getBestPrices())
        
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
case class PlaceBetEvent(betId: Long, userId: Long, betSize: Double, betPrice: Double, betType: BetTypeEnum, marketId: Long, runnerId: Long, placedDate: Long)
case class GetBestPricesEvent(marketId:Long)