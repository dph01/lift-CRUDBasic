package code.snippet

import net.liftweb._
import net.liftweb.http._
import util._
import common._
import Helpers._
import TimeHelpers._
import net.liftweb.common.Logger
import net.liftweb.mapper._
import scala.xml._
import code.model.Event

class EventOps extends Logger {
  
  // note that Event.create isn't being called here. Instead we're registering
  // which function eventVar calls to get a default value in the case that the
  // eventVar object is accessed before it is set
  object eventVar extends RequestVar[Event](Event.create)
 
  def processSubmit() = {
    // if the validate or submit fails, reload the current page
   eventVar.is.validate match {
        case  Nil => if ( eventVar.is.save ) { 
          S.notice("Event Saved")
          S.redirectTo("/event/listevent")
        } else {
          S.error("Failed to save event to database")
        }
      case errors => S.error(errors)
      }
  }
  
  def create = {
    // in the case that we're reloading the data from a previous form submission
    // the eventVar will have been injected from the previous snippet. In this case
    // we don't need to create a new Event
    // if (! eventVar.set_?) eventVar(Event.create)
    
    var event = eventVar.is
    debug("initial event name value: " + eventVar.is.eventName)
    
    "#hidden" #> SHtml.hidden(() => eventVar(event) ) &
    "#eventname" #> SHtml.text(eventVar.is.eventName, name => eventVar.is.eventName(name) ) &
    "#submit" #> SHtml.onSubmitUnit(processSubmit)
  }

  def edit = {
    if ( eventVar.set_? ) {
      val event = eventVar.is
      "#hidden" #> SHtml.hidden(() => eventVar(event) ) &
      "#eventname" #> SHtml.text(eventVar.is.eventName, name => eventVar.is.eventName(name) ) &
      "#submit" #> SHtml.onSubmitUnit(processSubmit)
    } else {
      "*" #> "Naviation Error. Access the Edit page through the List or View pages."
    }
    
  }

 def list = {
    val allEvents = Event.findAll
    ".row *" #> allEvents.map( t => {
      ".eventName *" #> Text(t.eventName) &
      ".actions *" #> {SHtml.link("/event/viewevent", () => eventVar(t), Text("view")) ++ Text(" ") ++
                 SHtml.link("/event/editevent", () => eventVar(t), Text("edit")) ++ Text(" ") ++
                 SHtml.link("/event/listevent", () => {t.delete_!}, Text("delete"))}
    } )          
  }
  def view = {
    if ( eventVar.set_? ) {
      var event = eventVar.is
      "#eventname *" #> eventVar.is.eventName.asHtml &
      "#edit" #> SHtml.link("/event/editevent", () => (eventVar(event)), Text("edit"))
    } else {
      "*" #> "Navigation Error. Access the View page through the List page."
    }
  }
 }
