package code.snippet


import scala.xml.NodeSeq
import scala.util.Random
import javax.servlet.http.Cookie
import net.liftweb.http.S
import net.liftweb.mockweb.WebSpec
import net.liftweb.mocks.MockHttpServletRequest
import net.liftweb.http.LiftRules
import net.liftweb.common.Logger
import net.liftweb.util.Helpers._
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
import org.scalatest.{ FunSuite, SuperSuite, BeforeAndAfterAll}
import net.liftweb.common.Box
import scala.collection.JavaConversions._
/*
import org.mortbay.jetty.{ Connector, Server}
import org.mortbay.jetty.webapp.{ WebAppContext }
import org.openqa.selenium.server.RemoteControlConfiguration
import org.openqa.selenium.server.SeleniumServer
import com.thoughtworks.selenium._
import com.sidewayscoding.model._
 */

class AddEventTest extends FunSuite with BeforeAndAfterAll with Logger {

  private var server : Server       = null
  private var selenium : WebDriver  = null
  private val GUI_PORT              = 8080
  private var host                  = "http://localhost:" + GUI_PORT.toString
  

  override def beforeAll() {
    /*  This code takes care of the following:
        1. Start an instance of your web application
        2. Start an instance of the Selenium client
    */

    // Setting up the jetty instance which will be running the
    // GUI for the duration of the tests
    server  = new Server(GUI_PORT)
    val context = new WebAppContext()
    context.setServer(server)
    context.setContextPath("/")
    context.setWar("src/main/webapp")
    server.addHandler(context)
    server.start()

    // Setting up the Selenium Client for the duration of the tests
    selenium = new HtmlUnitDriver();
    selenium.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
  }

  override def afterAll() {
    // Close everyhing when done
    debug("in afterAll")
    selenium.close()
    server.stop()
  }

  // a test to navigate to the list Events page, from there click the 'New Event' link
  // create a new Event entry, and then check that the new Event is on the 
  // list page 
  val listPageURL = host + "/event/listevent"
  val createPage = host + "/event/createevent"
  val listPageTitle = "App: List Events"
  // create random anem to avoid clashes with existing names
  val eventNameText = Random.nextString(8)
  
  test("From the list page add an new event" ) {
    // navigate to our list page
    selenium.get(listPageURL)
    
    // check that we are on the right page
    expect(listPageTitle) {selenium.getTitle}
    
    // check the link to create a new event exists and that it points to the right page
    // note that findElement throws an exception if it cannot find requested item
    var newEventLink: Box[WebElement] = tryo( selenium.findElement(By.linkText("New Event")) )
    assert(newEventLink.map(_.getAttribute("href")).openOr("") == createPage)
    
    // get the html node that contains the list of events
    var eventList: Box[WebElement] = tryo( selenium.findElement(By.id("eventlist")) )
    assert(! eventList.isEmpty )
    
    // get the <td class="eventName">..</td> elements within the list
    // note we've just asserted that eventList is not empty, so OK to use open_!
    var events : scala.collection.mutable.Buffer[WebElement]= 
        eventList.map(_.findElements(By.className("eventName"))).open_!
    
    // make sure none of them contain the new name
    assert(!events.map(_.getText).contains(eventNameText))
   
    // navigate to the new event page
    newEventLink.foreach(_.click)
   
    // locate the name input element
    var eventName: Box[WebElement] = tryo(selenium.findElement(By.id("eventname")))
    assert(!eventName.isEmpty)
    
    // enter the value text of the name and submit
    eventName.foreach(x => {
      x.sendKeys(eventNameText)
      x.submit
    })
    
    // we should now be back at the list page
    expect(listPageTitle) {selenium.getTitle}
    
    var eventList2: Box[WebElement] = tryo( selenium.findElement(By.id("eventlist")) )
    assert(! eventList2.isEmpty )
    
    // get the <td class="eventName">..</td> elements within the list
    // note we've just asserted that eventList is not empty, so OK to use open_!
    var events2 : scala.collection.mutable.Buffer[WebElement]= 
        eventList2.map(_.findElements(By.className("eventName"))).open_!
    
    // now make sure one of them contain the new name
    assert(events2.map(_.getText).contains(eventNameText))
  }
  
  test("Test that new name doesn't exist in current list") {
    selenium.get(listPageURL)
    var element: WebElement = selenium.findElement(By.id("eventlist"));
    // element.sendKeys("asdfasdfasdfasdfasdf");
    // element.submit
    // assert(selenium.isTextPresent("Found 0 matches to your criteria"), true)
    true
  }

  /*
  test("Test that you can create an entry and it will show up in search results") {
    selenium.open("/")
    selenium.click("//x:a[contains(@href, '/add')]")
    selenium.waitForPageToLoad("30000")
    selenium.`type`("name", entryName)
    selenium.`type`("description", entryDescription)
    selenium.click("EntrySubmissionButton")
    selenium.waitForPageToLoad("30000")
    assert(selenium.isTextPresent("Horray! The dictionary just grew bigger"), true)

    selenium.open("/")
    selenium.`type`("search", entryName)
    selenium.click("submit")
    selenium.waitForPageToLoad("30000")
    assert(selenium.isTextPresent("Found 1 matches to your criteria"), true)
    assert(selenium.isTextPresent(entryName), true)
    assert(selenium.isTextPresent(entryDescription), true)
  }
  */
}