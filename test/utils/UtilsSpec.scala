package utils

import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UtilsSpec extends Specification {

  "Utils" should {

    "have md5('md5') = '1bc29b36f623ba82aaf6724fd3b16718'" in {
      md5("md5").toLowerCase must equalTo("1bc29b36f623ba82aaf6724fd3b16718")
    }

    "issue a new OAuthConsumer with key = 'key' and secret = 'secret'" in {
      val consumer = oauthConsumer("key", "secret")
      consumer.getConsumerKey must equalTo("key")
      consumer.getConsumerSecret must equalTo("secret")
    }

    val request = "https://www.appdirect.com/rest/api/events/dummyChange?oauth_nonce=72250409&oauth_timestamp=1294966759&oauth_consumer_key=Dummy&oauth_signature_method=HMAC-SHA1&oauth_version=1.0&oauth_signature=IBlWhOm3PuDwaSdxE%2FQu4RKPtVE%3D"
    "validate OAuth request = " + request in {
      oauthValidate(oauthConsumer("Dummy", "secret"), url = request, None) should beTrue
    }

    val invalidRequest = "https://www.appdirect.com/rest/api/events/dummyChange?oauth_nonce=72250408&oauth_timestamp=1294966715&oauth_consumer_key=Dummy&oauth_signature_method=HMAC-SHA1&oauth_version=1.0&oauth_signature=IBlWhOm3PuDwaSdxE%2FQu4RKPtVE%3D"
    "not validate OAuth request = " + invalidRequest in {
      oauthValidate(oauthConsumer("Dummy", "secret"), url = invalidRequest, None) should beFalse
    }

  }

}
