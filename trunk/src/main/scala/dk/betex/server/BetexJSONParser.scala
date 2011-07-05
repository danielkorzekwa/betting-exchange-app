package dk.betex.server

import dk.betex.api._
import dk.betex.api.IMarket._
import org.codehaus.jettison.json._
import BetexActor._
import org.apache.commons.math.util._

/**
 * Converts betex data to json object.
 *
 * @author korzekwad
 */
object BetexJSONParser {

  def toJSON(data: Any): JSONObject = {
    data match {
      case d: Map[Long, Tuple2[IRunnerPrice, IRunnerPrice]] => toJSON(d)
      case d: List[IMarket] => toJSON(d)
      case d: IMarket => toJSON(d)
      case d: MarketProb => toJSON(d)
    }
  }

  /**Key - runnerId, Value - market prices (element 1 - priceToBack, element 2 - priceToLay)*/
  private def toJSON(marketPrices: Map[Long, Tuple2[IRunnerPrice, IRunnerPrice]]): JSONObject = {

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
    jsonObj
  }

  private def toJSON(markets: List[IMarket]): JSONObject = {
    val marketsObj = new JSONArray()
    for (market <- markets) {
      val marketObj = toJSON(market)
      marketsObj.put(marketObj)
    }

    val jsonObj = new JSONObject()
    jsonObj.put("markets", marketsObj)
    jsonObj

  }

  private def toJSON(market: IMarket): JSONObject = {
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

    marketObj
  }

  private def toJSON(marketProb: MarketProb): JSONObject = {
    val marketProbsArray = new JSONArray()

    for ((runnerId, runnerProb) <- marketProb.probs) {
      val runnerProbObj = new JSONObject()
      runnerProbObj.put("runnerId", runnerId)
      runnerProbObj.put("probability", MathUtils.round(runnerProb*100,2))
      marketProbsArray.put(runnerProbObj)
    }

    val marketProbsObj = new JSONObject()
    marketProbsObj.put("marketId", marketProb.marketId)
    marketProbsObj.put("probType", marketProb.probType.toString)
    marketProbsObj.put("probabilities", marketProbsArray)
    marketProbsObj
  }

}