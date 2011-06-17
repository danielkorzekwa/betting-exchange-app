package dk.betex.server

import com.sun.jersey.test.framework.JerseyTest
import org.junit._
import org.junit.Assert._
import com.sun.jersey.api.client.WebResource

/**http://10.2.4.191/createMarket?marketId=10&marketName=m1&eventName=e1&numOfWinners=1&marketTime=10&runners=11:r1,12:r2
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
    assertEquals("OK", responseMsg)

    val markets = webResource.path("/getMarkets").get(classOf[String])
    assertEquals("""{"markets":[{"marketId":123,"marketName":"Man Utd - Arsenal","eventName":"English Soccer","numOfWinners":1,"marketTime":1000,"runners":[{"runnerId":11,"runnerName":"Man Utd"},{"runnerId":12,"runnerName":"Arsenal"}]}]}""", markets)
  }

  @Test
  def create_two_market2_succces() {
    val webResource = resource()
    val responseMsg1 = createMarket(123)
    assertEquals("OK", responseMsg1)

    val responseMsg2 = createMarket(124)
    assertEquals("OK", responseMsg2)

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

  /**Test scenarios for placeBet.*/
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
    assertEquals("OK", responseMsg)

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
    assertEquals("OK", responseMsg)

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

    assertEquals("ERROR:key not found: 1", responseMsg)
  }

  /**Tests for getBestPrices.*/
  @Test
  def get_best_prices_market_doesnt_exists() {
    val webResource = resource()
    val bestPricesMsg = webResource.path("/getBestPrices").queryParam("marketId", "1").get(classOf[String])
    assertEquals("ERROR:key not found: 1", bestPricesMsg)
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
    assertEquals("ERROR:key not found: 1", cancelBetsMsg)
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
    assertEquals("OK", cancelBetsMsg)
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
    assertEquals("OK", cancelBetsMsg)
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
    assertEquals("OK", placeBetMsg)

    val cancelBetsMsg = webResource.path("/cancelBets").
      queryParam("userId", "1000").
      queryParam("betSize", "10").
      queryParam("betPrice", "2.2").
      queryParam("betType", "BACK").
      queryParam("marketId", "123").
      queryParam("runnerId", "12").get(classOf[String])
    assertEquals("OK", cancelBetsMsg)

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
    assertEquals("OK", placeBetMsg)

    val cancelBetsMsg = webResource.path("/cancelBets").
      queryParam("userId", "1000").
      queryParam("betSize", "10").
      queryParam("betPrice", "2.2").
      queryParam("betType", "LAY").
      queryParam("marketId", "123").
      queryParam("runnerId", "12").get(classOf[String])
    assertEquals("OK", cancelBetsMsg)

    val bestPricesMsg = webResource.path("/getBestPrices").queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"marketPrices":[{"runnerId":11},{"runnerId":12,"bestToBackPrice":2.2,"bestToBackTotal":20}]}""", bestPricesMsg)
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
}