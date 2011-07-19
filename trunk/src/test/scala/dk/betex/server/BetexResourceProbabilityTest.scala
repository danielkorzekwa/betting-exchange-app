package dk.betex.server

import com.sun.jersey.test.framework.JerseyTest
import org.junit._
import org.junit.Assert._
import com.sun.jersey.api.client.WebResource
import org.codehaus.jettison.json._

/**
 * @author korzekwad
 */
class BetexResourceProbabilityTest extends JerseyTest("dk.betex.server") {
 
  private val testUtil = new BetexResourceTestUtil(resource())
  import testUtil._
  
  /**Tests for getMarketProbability*/
  @Test
  def get_market_prob_market_not_found {

    val market = resource().path("/getMarketProbability").
      queryParam("marketId", "123").
      queryParam("probType", "WIN").
      get(classOf[String])
    assertEquals("""{"status":"ERROR:requirement failed: Market not found for marketId=123"}""", market)

  }

  @Test
  def get_market_prob_wrong_prob_type {
    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "123").
      queryParam("probType", "WIN_WRONG").
      get(classOf[String])
    assertEquals("""{"status":"INPUT_VALIDATION_ERROR:Incorrect probType = WIN_WRONG. Supported values = MarketProbTypeEnum [WIN, PLACE, SHOW]"}""", marketProb)
  }

  @Test
  def get_market_prob_win_market_win_prob {

    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,1)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "WIN").
      get(classOf[String])
    assertEquals("""{"marketId":1,"probType":"WIN","probabilities":[{"runnerId":11,"probability":10},{"runnerId":12,"probability":30},{"runnerId":13,"probability":30},{"runnerId":14,"probability":30}]}""", marketProb)
  }

  @Test
  def get_market_prob_win_market_default_prob_type {

    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,1)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      get(classOf[String])
    assertEquals("""{"marketId":1,"probType":"WIN","probabilities":[{"runnerId":11,"probability":10},{"runnerId":12,"probability":30},{"runnerId":13,"probability":30},{"runnerId":14,"probability":30}]}""", marketProb)
  }

  @Test
  def get_market_prob_win_market_place_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,1)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "PLACE").
      get(classOf[String])
    assertEquals("""{"marketId":1,"probType":"PLACE","probabilities":[{"runnerId":11,"probability":22.86},{"runnerId":12,"probability":59.05},{"runnerId":13,"probability":59.05},{"runnerId":14,"probability":59.05}]}""", marketProb)

  }

  @Test
  def get_market_prob_win_market_show_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,1)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "SHOW").
      get(classOf[String])
    assertEquals("""{"marketId":1,"probType":"SHOW","probabilities":[{"runnerId":11,"probability":42.14},{"runnerId":12,"probability":85.95},{"runnerId":13,"probability":85.95},{"runnerId":14,"probability":85.95}]}""", marketProb)

  }

  @Test
  def get_market_prob_place_market_place_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,2)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "PLACE").
      get(classOf[String])
    assertEquals("""{"marketId":1,"probType":"PLACE","probabilities":[{"runnerId":11,"probability":20},{"runnerId":12,"probability":60},{"runnerId":13,"probability":60},{"runnerId":14,"probability":60}]}""", marketProb)

  }

  @Test
  def get_market_prob_place_market_default_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,2)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      get(classOf[String])
    assertEquals("""{"marketId":1,"probType":"PLACE","probabilities":[{"runnerId":11,"probability":20},{"runnerId":12,"probability":60},{"runnerId":13,"probability":60},{"runnerId":14,"probability":60}]}""", marketProb)

  }

  @Test
  def get_market_prob_place_market_show_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,2)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "SHOW").
      get(classOf[String])
    assertEquals("""{"status":"ERROR:Can't calculate probability for numOfWinners=2 and probType=SHOW."}""", marketProb)

  }

  @Test
  def get_market_prob_place_market_win_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,2)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "WIN").
      get(classOf[String])
    assertEquals("""{"status":"ERROR:Can't calculate probability for numOfWinners=2 and probType=WIN."}""", marketProb)

  }

  @Test
  def get_market_prob_show_market_show_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,3)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "SHOW").
      get(classOf[String])
    assertEquals("""{"marketId":1,"probType":"SHOW","probabilities":[{"runnerId":11,"probability":30},{"runnerId":12,"probability":90},{"runnerId":13,"probability":90},{"runnerId":14,"probability":90}]}""", marketProb)

  }

  @Test
  def get_market_prob_show_market_default_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,3)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      get(classOf[String])
    assertEquals("""{"marketId":1,"probType":"SHOW","probabilities":[{"runnerId":11,"probability":30},{"runnerId":12,"probability":90},{"runnerId":13,"probability":90},{"runnerId":14,"probability":90}]}""", marketProb)

  }

  @Test
  def get_market_prob_show_market_win_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,3)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "WIN").
      get(classOf[String])
    assertEquals("""{"status":"ERROR:Can't calculate probability for numOfWinners=3 and probType=WIN."}""", marketProb)

  }

  @Test
  def get_market_prob_show_market_place_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,3)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "PLACE").
      get(classOf[String])
    assertEquals("""{"status":"ERROR:Can't calculate probability for numOfWinners=3 and probType=PLACE."}""", marketProb)

  }

  @Test
  def get_market_prob_show_4_winners_market_show_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,4)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "SHOW").
      get(classOf[String])
    assertEquals("""{"marketId":1,"probType":"SHOW","probabilities":[{"runnerId":11,"probability":40},{"runnerId":12,"probability":120},{"runnerId":13,"probability":120},{"runnerId":14,"probability":120}]}""", marketProb)

  }

  @Test
  def get_market_prob_show_4_winners_market_win_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,4)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "WIN").
      get(classOf[String])
    assertEquals("""{"status":"ERROR:Can't calculate probability for numOfWinners=4 and probType=WIN."}""", marketProb)

  }

  @Test
  def get_market_prob_show_4_winners_market_place_prob {
    /**Data setup*/
    val createMarketResp = createMarketWithRunnersAndBets(123,4)
    assertEquals("""{"status":"OK"}""", createMarketResp)

    /**Check market probability.*/

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "PLACE").
      get(classOf[String])
    assertEquals("""{"status":"ERROR:Can't calculate probability for numOfWinners=4 and probType=PLACE."}""", marketProb)

  }

  @Test
  def get_market_prob_check_runners_order {
    createMarketRandomOrderOfRunners(1)

    val marketProb = resource().path("/getMarketProbability").
      queryParam("marketId", "1").
      queryParam("probType", "PLACE").
      get(classOf[String])
    assertEquals("""{"marketId":1,"probType":"PLACE","probabilities":[{"runnerId":4,"probability":28.57},{"runnerId":1,"probability":28.57},{"runnerId":2,"probability":28.57},{"runnerId":7,"probability":28.57},{"runnerId":3,"probability":28.57},{"runnerId":5,"probability":28.57},{"runnerId":6,"probability":28.57}]}""", marketProb)
  }

}