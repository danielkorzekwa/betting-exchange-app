package dk.betex.server

import com.sun.jersey.test.framework.JerseyTest
import org.junit._
import org.junit.Assert._
import com.sun.jersey.api.client.WebResource
import org.codehaus.jettison.json._

/**
 * Test utilities for testing BetexResource Rest API.
 *
 * @author korzekwad
 */
class BetexResourceTestUtil(webResource: WebResource) {

  def createMarket(marketId: Long) {
    val response = webResource.path("/createMarket").
      queryParam("marketId", marketId.toString).
      queryParam("marketName", "Man Utd - Arsenal").
      queryParam("eventName", "English Soccer").
      queryParam("numOfWinners", "1").
      queryParam("marketTime", "1000").
      queryParam("runners", "11:Man Utd,12:Arsenal").
      get(classOf[String])

    assertEquals("""{"status":"OK"}""", response)
  }

  def createMarketRandomOrderOfRunners(marketId: Long) {
    val response = webResource.path("/createMarket").
      queryParam("marketId", marketId.toString).
      queryParam("marketName", "Man Utd - Arsenal").
      queryParam("eventName", "English Soccer").
      queryParam("numOfWinners", "1").
      queryParam("marketTime", "1000").
      queryParam("runners", "4:r4,1:r1,2:r2,7:r7,3:r3,5:r6,6:r6").
      get(classOf[String])

    assertEquals("""{"status":"OK"}""", response)
  }

  def createMarketWithRunnersAndBets(userId: Int, numOfWinners: Int) {
    val marketEvents = new JSONArray()
    marketEvents.put(new JSONObject("""{"time":1234567,"eventType":"CREATE_MARKET",
				"marketId":1, 
				"marketName":"Match Odds",
				"eventName":"HR 1", 
				"numOfWinners":%s, 
				"marketTime":
				"2010-04-15 14:00:00", 
				"runners": [{"runnerId":11,
				"runnerName":"Horse1"},
				{"runnerId":12, 
				"runnerName":"Horse2"},
    			{"runnerId":13, 
				"runnerName":"Horse3"},
    			{"runnerId":14, 
				"runnerName":"Horse4"}]
				}""".format(numOfWinners)))

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
    marketEventsData.put("userId", userId)
    marketEventsData.put("marketEvents", marketEvents)
    val response = webResource.path("/processBetexEvents").`type`("application/json").post(classOf[String], marketEventsData.toString)
    assertEquals("""{"status":"OK"}""", response)
  }

  def matchBets(userId: Int) {
    /**match bets.*/
    val marketEvents = new JSONArray()

    marketEvents.put(new JSONObject("""{"time":1234568,"eventType":"PLACE_BET",	
				"betSize":8,
				"betPrice":3,
				"betType":"BACK",
				"marketId":1,
				"runnerId":11
				} """))

    marketEvents.put(new JSONObject("""{"time":1234568,"eventType":"PLACE_BET",	
				"betSize":12,
				"betPrice":3.1,
				"betType":"LAY",
				"marketId":1,
				"runnerId":11
				} """))

    val marketEventsData = new JSONObject()
    marketEventsData.put("userId", userId)
    marketEventsData.put("marketEvents", marketEvents)
    val response = webResource.path("/processBetexEvents").`type`("application/json").post(classOf[String], marketEventsData.toString)
    assertEquals("""{"status":"OK"}""", response)
  }
}