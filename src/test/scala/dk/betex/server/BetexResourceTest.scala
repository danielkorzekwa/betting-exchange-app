package dk.betex.server

import com.sun.jersey.test.framework.JerseyTest
import org.junit._
import org.junit.Assert._
import com.sun.jersey.api.client.WebResource

class BetexResourceTest extends JerseyTest("dk.betex.server") {
  @Test
  def help() {
    val webResource = resource();
    val responseMsg = webResource.path("").get(classOf[String])
    assertTrue(responseMsg.contains("Betting Exchange Server Copyright 2011 Daniel Korzekwa(http://danmachine.com)"));
  }

  @Test
  def create_market() {
    val marketId = "123"
    val webResource = resource();
    val responseMsg = webResource.path("/createMarket").queryParam("marketId", marketId).get(classOf[String])
    assertEquals("Not implemented yet.MarketId=" + marketId, responseMsg);
  }

  @Test
  def get_markets() {
    val marketId = "123"
    val webResource = resource();
    val responseMsg = webResource.path("/getMarkets").get(classOf[String])
    assertEquals("Not implemented yet.", responseMsg);
  }
}