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

  }

}
