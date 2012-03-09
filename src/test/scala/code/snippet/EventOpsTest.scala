package code.snippet


import scala.xml.NodeSeq
import javax.servlet.http.Cookie
import net.liftweb.http.S
import net.liftweb.mockweb.WebSpec
import net.liftweb.mocks.MockHttpServletRequest
import net.liftweb.http.LiftRules
import net.liftweb.common.Logger
import org.openqa.selenium.server.RemoteControlConfiguration
import org.openqa.selenium.server.SeleniumServer
import org.mortbay.jetty.{Server,Connector}
import org.mortbay.jetty.servlet.ServletHolder
import org.mortbay.jetty.webapp.WebAppContext
import org.mortbay.jetty.nio.SelectChannelConnector
import org.specs2.mutable._
import net.liftweb.http.testing.{TestKit,ReportFailure,HttpResponse}
import com.thoughtworks.selenium.DefaultSelenium
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import java.util.concurrent.TimeUnit
/*
object CookieListSpec extends WebSpec with SetupAndTearDown {
  "CookieList Snippet" should {
    val cookieName = "thing"
        val r = new MockHttpServletRequest("/")
    r.cookies = List(new Cookie(cookieName, "value"))
    "List all cookies, separated by a break line" withSFor(r) in {
      //      val xml = S.runTemplate(List("event" , "createevent")) openOr NodeSeq.Empty
      //println("template path" + S.)
      val xml = S.runTemplate(List("index")) 
          println("xml: " + xml)
          // xml must \\(<div id="output">thing<br></br></div>)
          true
    }
  } }


trait SetupAndTearDown { _: WebSpec =>
setup().beforeSpec
def setup() = new bootstrap.liftweb.Boot().boot
def destroy() = LiftRules.unloadHooks.toList.foreach(_())
destroy().afterSpec
}
*/

object SeleniumTestServer {
  private val rc = new RemoteControlConfiguration
      rc.setPort(port)
      private val seleniumserver = new SeleniumServer(rc)
  lazy val port = System.getProperty(
      "selenium.server.port", "4444").toInt
      def start(){
    seleniumserver.boot()
    seleniumserver.start()
    seleniumserver.getPort()
  }
  def stop(){
    seleniumserver.stop()
  }
}


object JettyTestServer {
  private val server: Server = {
    val svr = new Server
        val connector = new SelectChannelConnector
        connector.setMaxIdleTime(30000);
    val context = new WebAppContext
        context.setServer(svr)
        context.setContextPath("/")
        context.setWar("src/main/webapp")
        svr.setConnectors(Array(connector));
    svr.addHandler(context)
    svr
}
lazy val port = server.getConnectors.head.getLocalPort
lazy val url = "http://localhost:" + port
def baseUrl = url
lazy val start = server.start()
def stop() = {
  server.stop()
  server.join()
} 
}

trait JettySetupAndTearDown {
  def setup() = JettyTestServer.start
      def destroy() = JettyTestServer.stop()
}

/*

trait SeleniumSetupAndTearDown extends JettySetupAndTearDown {
  _: Specification =>
override def setup() = {
    super.setup()
    SeleniumTestServer.start()
    Thread.sleep(1000)
    SeleniumTestClient.start()
}

override def destroy(){
  SeleniumTestClient.stop()
  Thread.sleep(1000)
  SeleniumTestServer.stop()
  super.destroy()
}
/*
object SeleniumTestClient {
  lazy val browser = new DefaultSelenium("localhost",
      SeleniumTestServer.port, "*firefox", JettyTestServer.url+"/")
  def start(){
    browser.start()
  }
  def stop(){
    browser.stop()
  }
} }
*/
*/
class SeleniumExampleSpec extends Specification
// class MyTest extends JettySetupAndTearDown with Logger
                          // with SeleniumSetupAndTearDown
                          with JettySetupAndTearDown
                          {
    
  "/event/listevent" should {
     // import SeleniumTestClient._
    "replace the button with text when clicked" in {
      /*
      browser.open("/testkit/ajax")
      browser.click("clickme")
      
      browser.waitForCondition("""
        selenium.browserbot
        .getCurrentWindow().document
        .getElementById('ajax_button')
        .innerHTML == 'Clicked'""",
        "1000")
      browser.isTextPresent("Clicked") mustBe true
      */
      // browser.open("/testkit/ajax")
             // Create a new instance of the Firefox driver
        // Notice that the remainder of the code relies on the interface, 
        // not the implementation.
      debug("calling setup 1")
      // setup
      
      // Thread.sleep(20000)
        debug("here 1")
        //val driver: WebDriver = new FirefoxDriver();
        val driver: WebDriver = new HtmlUnitDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        driver.get("http://localhost:8080/event/createevent");
        // expect("aApp: List Events") {driver.getTitle}

        // find the name input box and set contents
        var element: WebElement = driver.findElement(By.id("eventname"));
        element.sendKeys("bob");

        // Now submit the form. WebDriver will find the containing form for us from the element
        element.submit();

        println("Page title is: " + driver.getTitle());
        
        // Google's search is rendered dynamically with JavaScript.
        // Wait for the page to load, timeout after 10 seconds
        debug("here 7")
        /*
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition[Boolean]() {
            override def apply(d: WebDriver): Boolean = {
                return d.getTitle().toLowerCase().startsWith("cheese!");
            }
        });
        */

        // Should see: "cheese! - Google Search"
        debug("here 8")
        // System.out.println("Page title is: " + driver.getTitle());
        
        //Close the browser
        debug("here 9")
        // driver.quit();
        true
    }
  }
                          }