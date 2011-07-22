package dk.betex.server

import com.sun.jersey.test.framework.JerseyTest
import org.junit._
import org.junit.Assert._
import com.sun.jersey.api.client.WebResource
import org.codehaus.jettison.json._

/**
 * http://10.2.4.191/placeBet?userId=4&betSize=3&betPrice=2&betType=LAY&marketId=10&runnerId=11
 */
class BetexResourceMarketTest extends JerseyTest("dk.betex.server") {

  private val testUtil = new BetexResourceTestUtil(resource())
  import testUtil._

  /**Test scenarios for createMarket.*/

  @Test
  def create_market_succces() {
    
    createMarket(123)
   
    val markets = resource().path("/getMarkets").get(classOf[String])
    assertEquals("""{"markets":[{"marketId":123,"marketName":"Man Utd - Arsenal","eventName":"English Soccer","numOfWinners":1,"marketTime":1000,"runners":[{"runnerId":11,"runnerName":"Man Utd"},{"runnerId":12,"runnerName":"Arsenal"}]}]}""", markets)
  }

  @Test
  def create_two_market2_succces() {
  createMarket(123)
  createMarket(124)
    
    val markets = resource().path("/getMarkets").get(classOf[String])
    assertEquals("""{"markets":[{"marketId":124,"marketName":"Man Utd - Arsenal","eventName":"English Soccer","numOfWinners":1,"marketTime":1000,"runners":[{"runnerId":11,"runnerName":"Man Utd"},{"runnerId":12,"runnerName":"Arsenal"}]},{"marketId":123,"marketName":"Man Utd - Arsenal","eventName":"English Soccer","numOfWinners":1,"marketTime":1000,"runners":[{"runnerId":11,"runnerName":"Man Utd"},{"runnerId":12,"runnerName":"Arsenal"}]}]}""", markets)
  }

  @Test
  def create_market_failed_missing_market_id() {
   
    val responseMsg = resource().path("/createMarket").
      queryParam("marketName", "Barcelona - Real Madryt").
      queryParam("eventName", "Spanish Soccer").
      queryParam("numOfWinners", "1").
      queryParam("marketTime", "2000").
      queryParam("runners", "1:Barcelona,2:Real Madryt").
      get(classOf[String])
    assertEquals("""{"status":"INPUT_VALIDATION_ERROR:requirement failed"}""", responseMsg);
  }

  /**Tests for getMarkets.*/
  @Test
  def get_markets_no_markets_exist() {
   
    val markets = resource().path("/getMarkets").get(classOf[String])
    assertEquals("""{"markets":[]}""", markets)
  }

  /**Tests for getMarket.*/
  @Test
  def get_market() {
    createMarket(123)
   
    val market = resource().path("/getMarket").
      queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"marketId":123,"marketName":"Man Utd - Arsenal","eventName":"English Soccer","numOfWinners":1,"marketTime":1000,"runners":[{"runnerId":11,"runnerName":"Man Utd"},{"runnerId":12,"runnerName":"Arsenal"}]}""", market)
  }

  @Test
  def get_market_doesnt_exist() {
   
    val market = resource().path("/getMarket").
      queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"status":"ERROR:requirement failed: Market not found for marketId=123"}""", market)
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
    createMarket(123)
  
    val removeMarketStatus = resource().path("/removeMarket").
      queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"status":"OK"}""", removeMarketStatus)

    val market = resource().path("/getMarket").
      queryParam("marketId", "123").get(classOf[String])
    assertEquals("""{"status":"ERROR:requirement failed: Market not found for marketId=123"}""", market)

  }

  /**Tests for getBestPrices.*/
  @Test
  def get_best_prices_market_doesnt_exists() {
    val webResource = resource()
    val bestPricesMsg = webResource.path("/getBestPrices").queryParam("marketId", "1").get(classOf[String])
    assertEquals("""{"status":"ERROR:requirement failed: Market not found for marketId=1"}""", bestPricesMsg)
  }

  @Test
  def get_best_prices_check_order() {
    createMarketRandomOrderOfRunners(1)
    val webResource = resource()
    val bestPricesMsg = webResource.path("/getBestPrices").queryParam("marketId", "1").get(classOf[String])
    assertEquals("""{"marketPrices":[{"runnerId":4},{"runnerId":1},{"runnerId":2},{"runnerId":7},{"runnerId":3},{"runnerId":5},{"runnerId":6}]}""", bestPricesMsg)
  }

}