package code.snippet

import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions.asScalaBuffer
import scala.util.Random
import org.mortbay.jetty.webapp.WebAppContext
import org.mortbay.jetty.Server
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.{WebElement, WebDriver, By}
import org.scalatest.{FunSuite, BeforeAndAfterAll}
import net.liftweb.common.{Logger, Box}
import net.liftweb.util.Helpers.tryo

class EventOpsTest extends FunSuite with BeforeAndAfterAll with Logger {

  private var server : Server       = null
  private var selenium : WebDriver  = null
  private val GUI_PORT              = 8081
  private var host                  = "http://localhost:" + GUI_PORT.toString
  

  override def beforeAll() {
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
    selenium.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
  }

  override def afterAll() {
    // Close everyhing when done
    selenium.close()
    server.stop()
  }

  val listPageURL = host + "/event/listevent"
  val createPageURL = host + "/event/createevent"
  val editPageURL = host + "/event/editevent"
  val viewPageURL = host + "/event/viewevent"
  val deletePageURL = host + "/event/deleteevent"
  val listPageTitle = "App: List Events"
  val viewPageTitle = "App: View Event"
  val createPageTitle = "App: Create Event"
  val editPageTitle = "App: Edit Event"
  val deletePageTitle = "App: Delete Event"
    
 test("error message displayed if validation fails when creating a new event") {
    selenium.get(createPageURL)
    val createPage = new createEventPage(selenium)
    
    // names with less than three characters should fail validation
    createPage.setEventName("aa")
    createPage.submit
    
    // should now be back at create page with an error message
    val createPage2 = new createEventPage(selenium)
    assert(createPage2.containsErrorText("Event name must be more than 3 characters"))
    
  }
   // create random name to avoid clashes with existing names
  val eventNameText = randomName
  
  // probably best to split this into smaller tests, but it serves to illustrate 
  // how to use some features of selenium
  test("From the list page add, view, edit and delete a new event" ) {
    // navigate to our list page
    selenium.get(listPageURL)
    
    val listPage = new listEventPage( selenium )
    
    // make sure an event doesn't already exist with our new name
    val eventNames1 : List[String] = listPage.eventNames
    assert(!eventNames1.contains(eventNameText))
   
    // navigate to the create page
    listPage.clickNewEventLink
   
    val createPage = new createEventPage(selenium)
   
    debug("creating event with name : " + eventNameText )
    createPage.setEventName(eventNameText)
    createPage.submit
    
    // we should now be back at the list page
    val listPage2 = new listEventPage(selenium)
    var eventNames2 : List[String] = listPage2.eventNames

    // now make sure that the new events list differs by exactly one element
    // and that one element is our newly added event
    var eventNamesDiff = eventNames2.diff(eventNames1) ++ eventNames1.diff(eventNames2)
    expect(1) { eventNamesDiff.length }
    expect(eventNameText){ eventNamesDiff.head }
    
    // now navigate to the view page of the newly created event
    val rows = listPage2.rows(eventNameText)
    // this 'expect' should pass, given that the previous test passed
    expect(1){rows.length}
    
    // get the view link. We've just verified that rows has exactly one element, so 
    // OK to take the head
    val viewLink = rows.head.findElement(By.linkText("view"))
   
    // navigate to the view page
    viewLink.click
    
    val viewPage = new viewEventPage(selenium)
    
    viewPage.clickEditEVent
    
    // check we're now n the edit page, edit page is checked in detail later
    val editPage1 = new editEventPage( selenium )

    // navigate back to list page
    selenium.get(listPageURL)
    
    val listPage3 = new listEventPage( selenium )
    val rows3 = listPage3.rows(eventNameText)
    // check that our element is still there
    expect(1){rows3.length}
    // get the view link. We've just verified that rows has exactly one element, so 
    // OK to take the head
    val editLink = rows3.head.findElement(By.linkText("edit"))
    
    // now click on the edit link
    editLink.click
    
    val editPage2 = new editEventPage( selenium )

    // change the name and submit
    val newEventNameText = randomName
    debug("changing name to: " + newEventNameText )
    editPage2.setEventName(newEventNameText)
    editPage2.submit
    
    // should now be back at the list page
    val listPage4 = new listEventPage( selenium )

    // check we now have an event under the  new name
    // and that we don't have an event of the old name
    // note that this last condition won't necessarily be true, but as we're using randomly
    // generated names it will probably be true, which is good enough
    val eventNames4 : List[String] = listPage4.eventNames
    assert(eventNames4.contains(newEventNameText))
    assert(!eventNames4.contains(eventNameText))
    
    // now delete the event
    val rows4 = listPage4.rows(newEventNameText)
    expect(1){rows4.length}
    val delLink = rows4.head.findElement(By.linkText("delete"))
    delLink.click
    
    // should now be on the confirm delete page
    val deletePage4 = new deleteEventPage( selenium )
    
    // make sure the 'No' link works
    deletePage4.clickNoLink
    
     // should now be back at the list page
    val listPage5 = new listEventPage( selenium )

    // check that the event is still there 
    var eventNames5 : List[String] = listPage5.eventNames
    assert(eventNames5.contains(newEventNameText))
    
    // no delete again for good
    val rows5 = listPage5.rows(newEventNameText)
    expect(1){rows5.length}
    val delLink5 = rows5.head.findElement(By.linkText("delete"))
    delLink5.click
    
     // should now be on the confirm delete page
    val deletePage5 = new deleteEventPage( selenium )
    
    // make sure the 'No' link works
    deletePage5.clickYesLink
 
     // should now be back at the list page
    val listPage6 = new listEventPage( selenium )
    
    // check that the event has been deleted
    var eventNames6 : List[String] = listPage6.eventNames
    assert(!eventNames6.contains(newEventNameText))

    true
  }
  
  test("going straight to view page should take us back to list page") { 
    selenium.get(viewPageURL)
    var listPage = new listEventPage( selenium )
    true 
  }
  test("going straight to delete page should take us back to list page") { 
    selenium.get(deletePageURL)
    var listPage = new listEventPage( selenium )
    true 
  }
  test("going straight to edit page should take us back to list page") { 
    selenium.get(editPageURL)
    var listPage = new listEventPage( selenium )
    true 
  }

  // uses the Page Object design pattern
  // http://seleniumhq.org/docs/06_test_design_considerations.html#page-object-design-pattern
  class deleteEventPage( sel: WebDriver ) {
    
     // check that we are on the right page
    expect(deletePageTitle) {sel.getTitle}
    
    // check the links to confirm and cancel are present
    // note that findElement throws an exception if it cannot find requested item
    val yesLink =  sel.findElement(By.linkText("Yes" ))
    assert(yesLink.getAttribute("href").startsWith(listPageURL))
 
    val noLink: Box[WebElement] = tryo( sel.findElement(By.linkText("No")) )
    debug("yes no:" + noLink)
    assert(noLink.map(_.getAttribute("href")).openOr("").startsWith(listPageURL))
    
    def clickYesLink : Unit = yesLink.click
    def clickNoLink : Unit = noLink.foreach(_.click)
  }
    
  class editEventPage( sel: WebDriver ) {
    
     // check that we are on the right page
    expect(editPageTitle) {sel.getTitle}
    
    // confirm we have name input element
    val eventName: Box[WebElement] = tryo(sel.findElement(By.id("eventname")))
    assert(!eventName.isEmpty)
    
    def setEventName(eventNameText: String) : Unit = eventName.foreach( x => {
        x.clear
        x.sendKeys(eventNameText)
      }
    )
    
    def submit : Unit = eventName.foreach(x => { println("trying to submit: " + x); x.submit})
    
  }
    
  class listEventPage( sel: WebDriver ) {
     // check that we are on the right page
    expect(listPageTitle) {sel.getTitle}
    
    // check the link to create a new event exists and that it points to the right page
    // note that findElement throws an exception if it cannot find requested item
    val newEventLink: Box[WebElement] = tryo( sel.findElement(By.linkText("New Event")) )
    assert(newEventLink.map(_.getAttribute("href")).openOr("") == createPageURL)
    
    // get the html node that contains the list of events
    val eventListNode: Box[WebElement] = tryo( sel.findElement(By.id("eventlist")) )
    assert(! eventListNode.isEmpty )
    
    // get the <td class="eventName">..</td> elements within the list
    // note we've just asserted that eventList is not empty, so OK to use open_!
    def eventTDs : scala.collection.mutable.Buffer[WebElement]= 
        eventListNode.map(_.findElements(By.className("eventName"))).open_!
        
    def eventNames : List[String] = eventTDs.map(x=>x.getText()).toList
    
    // <tr class="row">
    //   <td class="eventName">xagpuoiere</td>
    //   <td class="actions">
    //      <a href="/event/viewevent?F1138216417523W04BVP=_">view</a> 
    //      <a href="/event/editevent?F1138216417524RYEUX0=_">edit</a> 
    //      <a href="/event/listevent?F11382164175254K5EL3=_">delete</a>
    //   </td>
    // </tr>
    private def eventRows: scala.collection.mutable.Buffer[WebElement]= 
        eventListNode.map(_.findElements(By.className("row"))).open_!
        
    // returns a list of all the HTML table rows that have the specified event name
    def rows( eventNameText: String ) : List[WebElement] = {
      eventRows.filter(r => {
        // for each row, find the <td> element and is its text the name we've just created
        r.findElement(By.className("eventName")).getText == eventNameText
      }).toList
    }
      
    def clickNewEventLink : Unit = newEventLink.foreach(_.click)
  }
  
  class createEventPage( val sel: WebDriver ) extends pageNotices {
    
     // check that we are on the right page
    expect(createPageTitle) {sel.getTitle}
    
    // ensure we have the name input element
    val eventName: Box[WebElement] = tryo(sel.findElement(By.id("eventname")))
    assert(!eventName.isEmpty)
    
    def setEventName(eventNameText: String) : Unit = eventName.foreach( _.sendKeys(eventNameText))
    
    def submit : Unit = eventName.foreach(_.submit)
  }
  
  class viewEventPage( sel: WebDriver ) {
     // check that we are on the right page
    expect(viewPageTitle) {sel.getTitle}
    
    // check that it contains the right content
    var viewEventName: Box[WebElement] = tryo(selenium.findElement(By.id("eventname")))
    expect(eventNameText) {viewEventName.map(x => x.getText)}
    
    // check the link to edit the event exists and that it points to the right page
    // note that findElement throws an exception if it cannot find requested item
    val editEventLink: Box[WebElement] = tryo( sel.findElement(By.linkText("edit")) )
    assert(editEventLink.map(_.getAttribute("href")).openOr("").startsWith(editPageURL))
    
    def clickEditEVent : Unit = editEventLink.foreach( _.click)
    
  }
  
  // a trait to mix into other pages to enable us to check if specified text appears as 
  // either error, 
  trait pageNotices {
      def sel: WebDriver
      def errorNotices : Box[WebElement] = tryo(selenium.findElement(By.id("lift__noticesContainer___error")))
      
      // check to see if any of the error message contains a specified text
      def containsErrorText(text: String) : Boolean = {
        val errorLIs : scala.collection.mutable.Buffer[WebElement] = {
          // needed to split this out in order to convince the scala type checker
          val emptyList = new java.util.Vector[WebElement]
          if ( errorNotices.isEmpty) emptyList else  errorNotices.map(x => x.findElements(By.tagName("li"))).open_!
        }
        // iterate over the error list elements testing to see if the text of any contains the input text
        errorLIs.map(_.getText).exists(s => s.contains(text))
      }
  }
    
  def randomName = {
      // create a vector of random numbers, where each number corresponds to
      // a valid ascii character between a to z
      val intVect = (for ( i <- 1 to 10 ) yield ('a'.toInt + Random.nextInt(26)))
     // convert this vector to a string 
      intVect.map(x => x.toChar).mkString
  }
}