package dk.betex.server

import com.sun.jersey.test.framework.JerseyTest
import org.junit._
import org.junit.Assert._
import com.sun.jersey.api.client.WebResource
import org.codehaus.jettison.json._

/**
 * @author korzekwad
 */
class BetexResourceRiskTest extends JerseyTest("dk.betex.server") {

  private val testUtil = new BetexResourceTestUtil(resource())
  import testUtil._

  /**Test scenarios for getRisk.*/
  @Test
  def get_risk_user_id_param_not_found {
    val riskResp = resource().path("/getRisk").
      queryParam("marketId", "1").
      get(classOf[String])
    assertEquals("""{"status":"INPUT_VALIDATION_ERROR:requirement failed: User id parameter not found."}""", riskResp)
  }
  @Test
  def get_risk_market_id_param_not_found {
    val riskResp = resource().path("/getRisk").
      queryParam("userId", "123").
      get(classOf[String])
    assertEquals("""{"status":"INPUT_VALIDATION_ERROR:requirement failed: Market id parameter not found."}""", riskResp)
  }

  @Test
  def get_risk_no_bets {
    createMarket(1)

    val riskResp = resource().path("/getRisk").
      queryParam("userId", "123").
      queryParam("marketId", "1").
      get(classOf[String])
    assertEquals("""{"userId":123,"marketId":1,"marketExpectedProfit":0,"runnerIfwins":[{"runnerId":11,"ifWin":0},{"runnerId":12,"ifWin":0}]}""", riskResp)
  }

  @Test
  def get_risk_matched_bets {
    createMarketWithRunnersAndBets(123, 1)
    matchBets(124)

    /**check risk.*/
    val riskResp = resource().path("/getRisk").
      queryParam("userId", "123").
      queryParam("marketId", "1").
      get(classOf[String])
    assertEquals("""{"userId":123,"marketId":1,"marketExpectedProfit":-2.73,"runnerIfwins":[{"runnerId":11,"ifWin":8.74},{"runnerId":12,"ifWin":-4},{"runnerId":13,"ifWin":-4},{"runnerId":14,"ifWin":-4}]}""", riskResp)
  }

  @Test
  def get_risk_check_runners_order {
    createMarketRandomOrderOfRunners(1)

    val webResource = resource()
    val risk = webResource.path("/getRisk").queryParam("userId", "123").queryParam("marketId", "1").get(classOf[String])
    assertEquals("""{"userId":123,"marketId":1,"marketExpectedProfit":0,"runnerIfwins":[{"runnerId":4,"ifWin":0},{"runnerId":1,"ifWin":0},{"runnerId":2,"ifWin":0},{"runnerId":7,"ifWin":0},{"runnerId":3,"ifWin":0},{"runnerId":5,"ifWin":0},{"runnerId":6,"ifWin":0}]}""", risk)
  }

  /**Test scenarios for hedge operation.*/

  @Test
  def hedge_market_id_param_not_found {
    val response = resource().path("/hedge").queryParam("userId", "123").queryParam("runnerId", "1").get(classOf[String])
    assertEquals("""{"status":"INPUT_VALIDATION_ERROR:requirement failed: Market id parameter not found."}""", response)
  }

  @Test
  def hedge_runner_id_param_not_found {
    val response = resource().path("/hedge").queryParam("userId", "123").queryParam("marketId", "1").get(classOf[String])
    assertEquals("""{"status":"INPUT_VALIDATION_ERROR:requirement failed: Runner id parameter not found."}""", response)
  }

  @Test
  def hedge_user_id_param_not_found {
    val response = resource().path("/hedge").queryParam("marketId", "1").get(classOf[String])
    assertEquals("""{"status":"INPUT_VALIDATION_ERROR:requirement failed: User id parameter not found."}""", response)
  }

  @Test
  def hedge_market_not_found {
    createMarketWithRunnersAndBets(123, 1)
    matchBets(124)

    /**hedge.*/
    val hedgeResponse = resource().path("/hedge").queryParam("userId", "124").queryParam("marketId", "2").queryParam("runnerId", "11").get(classOf[String])
    assertEquals("""{"status":"ERROR:requirement failed: Market not found for marketId=2"}""", hedgeResponse)
  }

  @Test
  def hedge_runner_not_found {
    createMarketWithRunnersAndBets(123, 1)
    matchBets(124)

    /**hedge.*/
    val hedgeResponse = resource().path("/hedge").queryParam("userId", "124").queryParam("marketId", "1").queryParam("runnerId", "15").get(classOf[String])
    assertEquals("""{"status":"ERROR:requirement failed: Runner not found:15"}""", hedgeResponse)
  }

  @Test
  def hedge_no_user_bets {
    createMarketWithRunnersAndBets(123, 1)

    /**hedge.*/
    val hedgeResponse = resource().path("/hedge").queryParam("userId", "124").queryParam("marketId", "1").queryParam("runnerId", "11").get(classOf[String])
    assertEquals("""{"status":"OK"}""", hedgeResponse)
  }

  @Test
  def hedge_simulate_param_not_found {

    createMarketWithRunnersAndBets(123, 1)
    matchBets(124)

    /**check risk.*/
    val riskResp = resource().path("/getRisk").
      queryParam("userId", "124").
      queryParam("marketId", "1").
      get(classOf[String])
    assertEquals("""{"userId":124,"marketId":1,"marketExpectedProfit":2.5,"runnerIfwins":[{"runnerId":11,"ifWin":-9.2},{"runnerId":12,"ifWin":3.8},{"runnerId":13,"ifWin":3.8},{"runnerId":14,"ifWin":3.8}]}""", riskResp)

    /**hedge.*/
    val hedgeResponse = resource().path("/hedge").queryParam("userId", "124").queryParam("marketId", "1").queryParam("runnerId", "11").get(classOf[String])
    assertEquals("""{"betSize":4.4,"betPrice":3,"betType":"BACK","marketId":1,"runnerId":11}""", hedgeResponse)

    /**check risk.*/
    val riskResp2 = resource().path("/getRisk").
      queryParam("userId", "124").
      queryParam("marketId", "1").
      get(classOf[String])
    assertEquals("""{"userId":124,"marketId":1,"marketExpectedProfit":-0.4,"runnerIfwins":[{"runnerId":11,"ifWin":-0.4},{"runnerId":12,"ifWin":-0.4},{"runnerId":13,"ifWin":-0.4},{"runnerId":14,"ifWin":-0.4}]}""", riskResp2)

    /**hedge again.*/
    val hedgeResponse2 = resource().path("/hedge").queryParam("userId", "124").queryParam("marketId", "1").queryParam("runnerId", "11").get(classOf[String])
    assertEquals("""{"status":"OK"}""", hedgeResponse2)

  }
}