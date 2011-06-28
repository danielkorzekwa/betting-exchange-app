package dk.betex.server

import com.sun.jersey.test.framework.JerseyTest
import org.junit._
import org.junit.Assert._
import com.sun.jersey.api.client.WebResource
import org.codehaus.jettison.json._

/**
 * http://10.2.4.191/createMarket?marketId=10&marketName=m1&eventName=e1&numOfWinners=1&marketTime=10&runners=11:r1,12:r2
 * http://10.2.4.191/placeBet?betId=4&betSize=3&betPrice=2&betType=LAY&marketId=10&runnerId=11
 */
class BetexResourceTest extends JerseyTest("dk.betex.server") {
  @Test
  def help() {
    val webResource = resource();
    val responseMsg = webResource.path("").get(classOf[String])
    assertTrue(responseMsg.contains("Betting Exchange Server Copyright 2011 Daniel Korzekwa(http://danmachine.com)"));
  }

  /**Test scenarios for createMarket.*/

  @Test
  def create_market_succces() {
    val webResource = resource()
    val responseMsg = createMarket(123)
    assertEquals("""{"status":"OK"}""", responseMsg)

    val markets = webResource.path("/getMarkets").get(classOf[String])
    assertEquals("""{"markets":[{"marketId":123,"marketName":"Man Utd - Arsenal","eventName":"English Soccer","numOfWinners":1,"marketTime":1000,"runners":[{"runnerId":11,"runnerName":"Man Utd"},{"runnerId":12,"runnerName":"Arsenal"}]}]}""", markets)
  }

  @Test
  def create_two_market2_succces() {
    val webResource = resource()
    val responseMsg1 = createMarket(123)
    assertEquals("""{"status":"OK"}""", responseMsg1)

    val responseMsg2 = createMarket(124)
    assertEquals("""{"status":"OK"}""", responseMsg2)

    val markets = webResource.path("/getMarkets").get(classOf[String])
    assertEquals("""{"markets":[{"marketId":124,"marketName":"Man Utd - Arsenal","eventName":"English Soccer","numOfWinners":1,"marketTime":1000,"runners":[{"runnerId":11,"runnerName":"Man Utd"},{"runnerId":12,"runnerName":"Arsenal"}]},{"marketId":123,"marketName":"Man Utd - Arsenal","eventName":"English Soccer","numOfWinners":1,"marketTime":1000,"runners":[{"runnerId":11,"runnerName":"Man Utd"},{"runnerId":12,"runnerName":"Arsenal"}]}]}""", markets)
  }

  @Test
  def create_market_failed_missing_market_id() {
    val webResource = resource()
    val responseMsg = webResource.path("/createMarket").
      queryParam("marketName", "Barcelona - Real Madryt").
      queryParam("eventName", "Spanish Soccer").
      queryParam("numOfWinners", "1").
      queryParam("marketTime", "2000").
      queryParam("runners", "1:Barcelona,2:Real Madryt").
      get(classOf[String])
    assertEquals("INPUT_VALIDATION_ERROR:requirement failed", responseMsg);
  }

  /**Tests for getMarkets.*/
  @Test
  def get_markets_no_markets_exist() {
    val webResource = resource()
    val markets = webResource.path("/getMarkets").get(classOf[String])
    assertEquals("""{"markets":[]}""", markets)
  }

  /**Tests for getMarket.*/
  @Test
  def get_market() {
    val responseMsg1 = createMarket(123)
    assertEquals("""{"status":"OK"}""", responseMsg1)

    val market = resource().path("/getMarket").
      queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"marketId":123,"marketName":"Man Utd - Arsenal","eventName":"English Soccer","numOfWinners":1,"marketTime":1000,"runners":[{"runnerId":11,"runnerName":"Man Utd"},{"runnerId":12,"runnerName":"Arsenal"}]}""", market)
  }

  @Test
  def get_market_doesnt_exist() {
    val webResource = resource()
    val market = webResource.path("/getMarket").
      queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"status":"ERROR:key not found: 123"}""", market)
  }

  /**Tests for removeMarkets*/
  @Test
  def remove_market_doesnt_exist {
    val removeMarketStatus = resource().path("/removeMarket").
      queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"status":"OK"}""", removeMarketStatus)
  }

  @Test
  def remove_market {
    val responseMsg1 = createMarket(123)
    assertEquals("""{"status":"OK"}""", responseMsg1)

    val removeMarketStatus = resource().path("/removeMarket").
      queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"status":"OK"}""", removeMarketStatus)

    val market = resource().path("/getMarket").
      queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"status":"ERROR:key not found: 123"}""", market)

  }

  /**Test scenarios for placeBet*/
  @Test
  def place_back_bet() {
    createMarket(1)

    val webResource = resource()
    val responseMsg = webResource.path("/placeBet").
      queryParam("betId", "100").
      queryParam("userId", "1000").
      queryParam("betSize", "10").
      queryParam("betPrice", "2.2").
      queryParam("betType", "BACK").
      queryParam("marketId", "1").
      queryParam("runnerId", "11").
      queryParam("placedDate", "40000").get(classOf[String])
    assertEquals("""{"status":"OK"}""", responseMsg)

    val bestPricesMsg = webResource.path("/getBestPrices").queryParam("marketId", "1").get(classOf[String])
    assertEquals("""{"marketPrices":[{"runnerId":11,"bestToLayPrice":2.2,"bestToLayTotal":10},{"runnerId":12}]}""", bestPricesMsg)
  }

  @Test
  def place_lay_bet() {
    createMarket(1)

    val webResource = resource()
    val responseMsg = webResource.path("/placeBet").
      queryParam("betId", "100").
      queryParam("userId", "1000").
      queryParam("betSize", "10").
      queryParam("betPrice", "2.2").
      queryParam("betType", "LAY").
      queryParam("marketId", "1").
      queryParam("runnerId", "11").
      queryParam("placedDate", "40000").get(classOf[String])
    assertEquals("""{"status":"OK"}""", responseMsg)

    val bestPricesMsg = webResource.path("/getBestPrices").queryParam("marketId", "1").get(classOf[String])
    assertEquals("""{"marketPrices":[{"runnerId":11,"bestToBackPrice":2.2,"bestToBackTotal":10},{"runnerId":12}]}""", bestPricesMsg)
  }

  @Test
  def place_back_bet_wrong_bet_type() {
    val webResource = resource()
    val responseMsg = webResource.path("/placeBet").
      queryParam("betId", "100").
      queryParam("userId", "1000").
      queryParam("betSize", "10").
      queryParam("betPrice", "2.2").
      queryParam("betType", "BACK_WRONG").
      queryParam("marketId", "1").
      queryParam("runnerId", "11").
      queryParam("placedDate", "40000").get(classOf[String])

    assertEquals("INPUT_VALIDATION_ERROR:None.get", responseMsg)
  }

  @Test
  def place_back_bet_market_doesnt_exists() {
    val webResource = resource()
    val responseMsg = webResource.path("/placeBet").
      queryParam("betId", "100").
      queryParam("userId", "1000").
      queryParam("betSize", "10").
      queryParam("betPrice", "2.2").
      queryParam("betType", "BACK").
      queryParam("marketId", "1").
      queryParam("runnerId", "11").
      queryParam("placedDate", "40000").get(classOf[String])

    assertEquals("""{"status":"ERROR:key not found: 1"}""", responseMsg)
  }

  /**Tests for getBestPrices.*/
  @Test
  def get_best_prices_market_doesnt_exists() {
    val webResource = resource()
    val bestPricesMsg = webResource.path("/getBestPrices").queryParam("marketId", "1").get(classOf[String])
    assertEquals("""{"status":"ERROR:key not found: 1"}""", bestPricesMsg)
  }
  
  @Test
  def get_best_prices_check_order() {
    createMarketRandomOrderOfRunners(1)
    val webResource = resource()
    val bestPricesMsg = webResource.path("/getBestPrices").queryParam("marketId", "1").get(classOf[String])
    assertEquals("""{"marketPrices":[{"runnerId":4},{"runnerId":1},{"runnerId":2},{"runnerId":7},{"runnerId":3},{"runnerId":5},{"runnerId":6}]}""", bestPricesMsg)
  }

  /**Tests for cancelBets*/
  @Test
  def cancel_bets_market_doesnt_exists() {
    val webResource = resource()
    val cancelBetsMsg = webResource.path("/cancelBets").
      queryParam("userId", "100").
      queryParam("betSize", "10").
      queryParam("betPrice", "2.2").
      queryParam("betType", "BACK").
      queryParam("marketId", "1").
      queryParam("runnerId", "11").get(classOf[String])
    assertEquals("""{"status":"ERROR:key not found: 1"}""", cancelBetsMsg)
  }

  @Test
  def cancel_bets_runner_doesnt_exists() {
    createMarket(123)
    val webResource = resource()
    val cancelBetsMsg = webResource.path("/cancelBets").
      queryParam("userId", "100").
      queryParam("betSize", "10").
      queryParam("betPrice", "2.2").
      queryParam("betType", "BACK").
      queryParam("marketId", "123").
      queryParam("runnerId", "13").get(classOf[String])
    assertEquals("""{"status":"OK"}""", cancelBetsMsg)
  }

  @Test
  def cancel_bets_nothing_to_cancelled() {
    createMarket(123)
    val webResource = resource()
    val cancelBetsMsg = webResource.path("/cancelBets").
      queryParam("userId", "100").
      queryParam("betSize", "10").
      queryParam("betPrice", "2.2").
      queryParam("betType", "BACK").
      queryParam("marketId", "123").
      queryParam("runnerId", "11").get(classOf[String])
    assertEquals("""{"status":"OK"}""", cancelBetsMsg)
  }

  @Test
  def cancel_bets_back_bet_is_cancelled_partially() {
    createMarket(123)
    val webResource = resource()

    val placeBetMsg = webResource.path("/placeBet").
      queryParam("betId", "100").
      queryParam("userId", "1000").
      queryParam("betSize", "30").
      queryParam("betPrice", "2.2").
      queryParam("betType", "BACK").
      queryParam("marketId", "123").
      queryParam("runnerId", "12").
      queryParam("placedDate", "40000").get(classOf[String])
    assertEquals("""{"status":"OK"}""", placeBetMsg)

    val cancelBetsMsg = webResource.path("/cancelBets").
      queryParam("userId", "1000").
      queryParam("betSize", "10").
      queryParam("betPrice", "2.2").
      queryParam("betType", "BACK").
      queryParam("marketId", "123").
      queryParam("runnerId", "12").get(classOf[String])
    assertEquals("""{"status":"OK"}""", cancelBetsMsg)

    val bestPricesMsg = webResource.path("/getBestPrices").queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"marketPrices":[{"runnerId":11},{"runnerId":12,"bestToLayPrice":2.2,"bestToLayTotal":20}]}""", bestPricesMsg)
  }

  @Test
  def cancel_bets_lay_bet_is_cancelled_partially() {
    createMarket(123)
    val webResource = resource()

    val placeBetMsg = webResource.path("/placeBet").
      queryParam("betId", "100").
      queryParam("userId", "1000").
      queryParam("betSize", "30").
      queryParam("betPrice", "2.2").
      queryParam("betType", "LAY").
      queryParam("marketId", "123").
      queryParam("runnerId", "12").
      queryParam("placedDate", "40000").get(classOf[String])
    assertEquals("""{"status":"OK"}""", placeBetMsg)

    val cancelBetsMsg = webResource.path("/cancelBets").
      queryParam("userId", "1000").
      queryParam("betSize", "10").
      queryParam("betPrice", "2.2").
      queryParam("betType", "LAY").
      queryParam("marketId", "123").
      queryParam("runnerId", "12").get(classOf[String])
    assertEquals("""{"status":"OK"}""", cancelBetsMsg)

    val bestPricesMsg = webResource.path("/getBestPrices").queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"marketPrices":[{"runnerId":11},{"runnerId":12,"bestToBackPrice":2.2,"bestToBackTotal":20}]}""", bestPricesMsg)
  }

  /**Test scenarios for process Betex events*/
  @Test
  def process_betex_events {

    val marketEvents = new JSONArray()
    marketEvents.put(new JSONObject("""{"time":1234567,"eventType":"CREATE_MARKET",
				"marketId":1, 
				"marketName":"Match Odds",
				"eventName":"Man Utd vs Arsenal", 
				"numOfWinners":1, 
				"marketTime":
				"2010-04-15 14:00:00", 
				"runners": [{"runnerId":11,
				"runnerName":"Man Utd"},
				{"runnerId":12, 
				"runnerName":"Arsenal"}]
				}"""))

    marketEvents.put(new JSONObject("""{"time":1234568,"eventType":"PLACE_BET",	
				"betSize":10,
				"betPrice":3,
				"betType":"LAY",
				"marketId":1,
				"runnerId":11
				} """))

    marketEvents.put(new JSONObject("""{"time":1234568,"eventType":"PLACE_BET",	
				"betSize":20,
				"betPrice":3.1,
				"betType":"BACK",
				"marketId":1,
				"runnerId":11
				} """))

    marketEvents.put(new JSONObject("""{"time":12345611,"eventType":"CANCEL_BETS","betsSize":3.0,"betPrice":3,"betType":"LAY","marketId":1,"runnerId":11}"""))

    val marketEventsData = new JSONObject()
    marketEventsData.put("userId", 123)
    marketEventsData.put("marketEvents", marketEvents)
    val webResource = resource().path("/processBetexEvents").`type`("application/json").post(classOf[String], marketEventsData.toString)
    assertEquals("""{"status":"OK"}""", webResource)

    val bestPricesMsg = resource().path("/getBestPrices").queryParam("marketId", "1").get(classOf[String])
    assertEquals("""{"marketPrices":[{"runnerId":11,"bestToBackPrice":3,"bestToBackTotal":7,"bestToLayPrice":3.1,"bestToLayTotal":20},{"runnerId":12}]}""", bestPricesMsg)
  }

  private def createMarket(marketId: Long): String = {
    val webResource = resource()
    val responseMsg = webResource.path("/createMarket").
      queryParam("marketId", marketId.toString).
      queryParam("marketName", "Man Utd - Arsenal").
      queryParam("eventName", "English Soccer").
      queryParam("numOfWinners", "1").
      queryParam("marketTime", "1000").
      queryParam("runners", "11:Man Utd,12:Arsenal").
      get(classOf[String])

    responseMsg
  }
  
  private def createMarketRandomOrderOfRunners(marketId: Long): String = {
    val webResource = resource()
    val responseMsg = webResource.path("/createMarket").
      queryParam("marketId", marketId.toString).
      queryParam("marketName", "Man Utd - Arsenal").
      queryParam("eventName", "English Soccer").
      queryParam("numOfWinners", "1").
      queryParam("marketTime", "1000").
      queryParam("runners", "4:r4,1:r1,2:r2,7:r7,3:r3,5:r6,6:r6").
      get(classOf[String])

    responseMsg
  }
}