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
/*
 * This class provides the snippets that back the Event CRUD 
 * pages (List, Create, View, Edit, Delete)
 */
class EventOps extends Logger {
  
  // The eventVar object is used to pass Event instances between successive HTML requests 
  // in the following cases:
  // - when a create or edit form is submitted 
  // - from a list page to a delete, view or edit page
  // - from a view page to a edit page
  // Note that Event.create isn't being called here. Rather, we're registering
  // a function for eventVar to call to get a default value in the case that the
  // eventVar object is accessed before it is set.
  object eventVar extends RequestVar[Event](Event.create)
 
  // processSubmit is called by the both the create and edit forms when the user clicks submit
  // If the entered data passes validation then the user is redirected to the List page, 
  // otherwise the form on which the user clicked submit is reloaded 
  def processSubmit() = {
    eventVar.is.validate match {
      case  Nil => {
          // note the return boolean from eventVar.is.save is ignored as it does
          // not contain useful information. See thread:
          // https://groups.google.com/forum/?hl=en#!searchin/liftweb/Mapper.save/liftweb/kcWwaqGamW0/RjWfdOxjShEJ
          eventVar.is.save 
          S.notice("Event Saved")
          // S.seeOther throws an exception to interrupt the flow of execution and redirect
          // the user to the specified URL
          S.seeOther("/event/listevent")
        } 
      case errors => S.error(errors)
    }
    // exiting the function here causes the form to reload
  }
  
  // called from /event/createevent.html
  def create = {
    // When create is called on the first GET request, eventVar.is initialises eventVar by calling Event.create.
    // If create is being called due to a form reload after validation has failed on a PUT request, 
    // eventVar.is returns the event that was previously set by the SHtml.hidden function (see below)
    val event = eventVar.is
   //  SHtml.hidden registers a function that will be called when the form is submitted
    // In this case it sets the eventVar in the subsequent PUT request to contain the event that's been
    // created in this request. This line isn't strictly necessary as if it was omitted, on the subsequent
    // request, a new event instance would be created, and the name member variable on this new instance
    // would be set to contain the submitted name (see comments below). However, including this line 
    // avoids creating a new event object every time, and hence avoid unnecessary GC.
    // Notice that SHtml.hidded calls 'eventVar(event)' not 'eventVar(eventVar.is)'
    // this is because we want the eventVar.is function to be evaluated in the scope of the initial request
    // not the scope of the subsequent request.
    "#hidden" #> SHtml.hidden(() => eventVar(event) ) &
    // When the page is rendered: 
    // "SHtml.text(eventVar.is.eventName" - this sets the value of the event name HTML input field to the current 
    // value of the Event.eventName member held in the eventVar.
    // "name => eventVar.is.eventName(name)" - this registers a function that Lift will call
    // when the user clicks submit. This function sets Event.eventName in the eventVar
    // in the subsequent request to the value the user entered in the HTML name input field 
    "#eventname" #> SHtml.text(eventVar.is.eventName, name => eventVar.is.eventName(name) ) &
    // This registers a function for Lift to call when the user clicks submit
    "#submit" #> SHtml.onSubmitUnit(processSubmit)
  }
  
 // called from /event/editevent.html
  def edit = {
    // the editevent.html page should only be reached by the user clicking on links from the 
    // list or view page. The eventVar gets set when either of those links is clicked.
    if ( ! eventVar.set_? ) 
      S.redirectTo("/event/listevent")
      
    val event = eventVar.is
    // We're using the SHtml.hidden technique similarly to the create method described above.
    // However, in this case, it is necessary to include this line.
    // As we're editing an existing event, this event instance has an 'id' member variable
    // (Event inherits from IdPK) which was assigned a value when the event instance
    // was first saved to the database. If we omitted the following line, when the user
    // clicks submit, the subsequent request would create a new event instance
    // which would have an uninitialised id, and so when saved to the database would result
    // in a second database instance of the event being created.
    "#hidden" #> SHtml.hidden(() => eventVar(event) ) &
    "#eventname" #> SHtml.text(eventVar.is.eventName, name => eventVar.is.eventName(name) ) &
    "#submit" #> SHtml.onSubmitUnit(processSubmit)
  }

  // called from /event/listevent.html
 def list = {
   // for each Event object in the database render the name along with links to view, edit, and delete
    ".row *" #> Event.findAll.map( t => {
      ".eventName *" #> Text(t.eventName) &
      // With each link we're registering a function ()=>eventVar(t). If the user
      // clicks on this link, Lift calls this function before the subsequent page loads. This
      // sets the eventVar for that page load to contain the the relevant event instance.
      ".actions *" #> {SHtml.link("/event/viewevent", () => eventVar(t), Text("view")) ++ Text(" ") ++
                 SHtml.link("/event/editevent", () => eventVar(t), Text("edit")) ++ Text(" ") ++
                 SHtml.link("/event/deleteevent", () => eventVar(t), Text("delete"))}
    } )          
  }

  // called from /event/viewevent.html
  def view = {
    // we're expecting the eventVar to have been set when we were linked to from the list page
    if ( ! eventVar.set_? )
      S.redirectTo("/event/listevent")

    var event = eventVar.is
    "#eventname *" #> eventVar.is.eventName.asHtml &
    "#edit" #> SHtml.link("/event/editevent", () => (eventVar(event)), Text("edit"))
  }
  
  // called from /event/deleteevent.html
  def delete = {
    // we're expecting the eventVar to have been set when we were linked to from the list page
   if ( ! eventVar.set_? ) 
      S.redirectTo("/event/listevent")
      
   var e = eventVar.is
   "#eventname" #> eventVar.is.eventName &
   "#yes" #> SHtml.link("/event/listevent", () =>{ e.delete_!}, Text("Yes")) &
   "#no" #> SHtml.link("/event/listevent", () =>{ }, Text("No")) 
  }
 }
