package dk.betex.server

import com.sun.jersey.test.framework.JerseyTest
import org.junit._
import org.junit.Assert._
import com.sun.jersey.api.client.WebResource
import org.codehaus.jettison.json._

/**
 * @author korzekwad
 */
class BetexResourceTest extends JerseyTest("dk.betex.server") {

  @Test
  def help() {
    val webResource = resource();
    val responseMsg = webResource.path("").get(classOf[String])
    assertTrue(responseMsg.contains("Betting Exchange Server Copyright 2011 Daniel Korzekwa(http://danmachine.com)"));
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
}