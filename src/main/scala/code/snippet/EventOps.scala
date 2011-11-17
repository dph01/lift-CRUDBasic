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
  
  object eventVar extends RequestVar[Event](null)
 
  def processSubmit() = {
    // add code to do validations etc
     eventVar.save
     S.redirectTo("/event/listevent")
  }
  
  def create = {
    eventVar(Event.create)
    var event = eventVar.is
    "#hidden" #> SHtml.hidden(() => eventVar(event) ) &
    "#eventname" #> eventVar.is.eventName.toForm &
    "#submit" #> SHtml.onSubmitUnit(processSubmit)
  }

  def edit = {
   val event = eventVar.is
   "#hidden" #> SHtml.hidden(() => eventVar(event) ) &
   "#eventname" #> eventVar.is.eventName.toForm &
   "#submit" #> SHtml.onSubmitUnit(processSubmit)
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
    var event = eventVar.is
    "#eventname *" #> eventVar.is.eventName.asHtml &
    "#edit" #> SHtml.link("/event/editevent", () => (eventVar(event)), Text("edit"))
  }
 }
