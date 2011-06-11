package dk.betex.server

import com.sun.jersey.test.framework.JerseyTest
import org.junit._
import org.junit.Assert._
import com.sun.jersey.api.client.WebResource

class BetexResourceTest extends JerseyTest("dk.betex.server"){
  @Test def help() {
     val webResource = resource();
        val responseMsg = webResource.path("").get(classOf[String])
        assertEquals("Help.", responseMsg);
  }
  
   @Test def create_market() {
     val marketId="123"
     val webResource = resource();
        val responseMsg = webResource.path("/createMarket").queryParam("marketId",marketId).get(classOf[String])
        assertEquals("Not implemented yet.MarketId=" + marketId, responseMsg);
  }
   
    @Test def get_markets() {
     val marketId="123"
     val webResource = resource();
        val responseMsg = webResource.path("/getMarkets").get(classOf[String])
        assertEquals("Not implemented yet.",responseMsg);
  }
}