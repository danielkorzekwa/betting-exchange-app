package dk.betex.server

import com.sun.jersey.test.framework.JerseyTest
import org.junit._
import org.junit.Assert._
import com.sun.jersey.api.client.WebResource
import org.codehaus.jettison.json._

/**
 * http://10.2.4.191/placeBet?userId=4&betSize=3&betPrice=2&betType=LAY&marketId=10&runnerId=11
 */
class BetexResourceBetTest extends JerseyTest("dk.betex.server") {

  private val testUtil = new BetexResourceTestUtil(resource())
  import testUtil._

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

    assertEquals("""{"status":"INPUT_VALIDATION_ERROR:Incorrect betType = BACK_WRONG. Supported values = BetTypeEnum [BACK, LAY]"}""", responseMsg)
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

    assertEquals("""{"status":"ERROR:requirement failed: Market not found for marketId=1"}""", responseMsg)
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
    assertEquals("""{"status":"ERROR:requirement failed: Market not found for marketId=1"}""", cancelBetsMsg)
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

}