This article describes a simple approach for creating your own CRUD screens with Lift ( 
'CRUD' being the common acronym for the database operations of Create, Read, Update, Delete).

## Why build your own CRUD screens?
Lift already has several useful libraries that automatically generate CRUD functionality for database classes
e.g. CRUDify, LiftScreen, Mapper.toForm etc. So, why would you want to create CRUD screens yourself?

The CRUD functionality that comes for free in Lift's libraries is excellent for rapid prototyping, 
or even for use in production where your requirements aren't too complicated. 

The libraries have lots of usable, sensible defaults that gets you going quickly, potentially 
saving you lots of boiler plate code. 
If what you want doesn't quite match what Lift provides, 
thanks to thoughtful class design and useful Scala language features you can easily override much of the 
default functionality.

However, as your requirements grow you'll probably find your needs diverging more and more from 
the functionality the Lift libraries provide, and consequently needing to override 
more and more of the default code.

You may want, for example, more control over the layout of the fields on the screen.
You may want to combine combine multiple entities on the a single screen.
You may want more control over the processing logic invoked on a form submit.
Adding so much overriding code could easily leave you with
overly bloated classes, 
trying to coerce the Lift libraries to perform in ways that they were not designed to perform.

When this happens it often makes more sense to write your own CRUD code from the ground up.

## What is covered?
This article describes conventions for 
organising the required code into methods, classes, files and folders; naming conventions; and a simple screen flow model.

It gives example code for how to perform many of the common CRUD based tasks such as
managing state on the server between successive page requests, rendering HTML from the data
stored in the database, and so on.

I illustrate these principles using simple single database entity called Event, 
with a single field called 'eventName'.
I use the Lift Mapper library to persist and retrieve data from the database, but the general principles could 
be used equally well with Lift's Record library, or any other ORM for that matter.

## Prerequisites
I assume the reader is familiar with basic Lift concepts. 
As a minimum I would recommend the chapters 1-4 & 7 of [Simply Lift](http://stable.simply.liftweb.net/)
and / or the first eight chapters of [Exploring Lift](http://stable.simply.liftweb.net), 
up to the section on Mapper.

Most of the techniques I discuss here are presented in the above two works. 
I'm simply highlighting how to bring these techniques together 
in order to implement easily DIY CRUD functionality.

## Download the code
Download the code discussed in this article from http://www.github.com/dph01/lift-CRUDBasic

## Running Version
To see a running version of this code, go to [www.damianhelme.com/crudbasic](www.damianhelme.com/crudbasic)

## Overview
This user interacts with each data entity has a single HTML page for each of the following: 

  * creating a new instance
  * listing all existing instances
  * editing an existing instance
  * viewing (i.e. read only) an existing instance 
  * deleting an existing instance.

The diagram below shows the allowable page transitions, illustrated with the Event example. 
We enter the screen flow through either the list or create
page and we can only navigate to the view, edit and delete pages through links on one of the other pages.

[screen flow TBC]

The code is split over a model file, a snippet file and series of HTML templates as shown below:

[folder structure TBC]

## Model
We use Lift's Mapper class to manage database access. Each entity has its own scala file in the model package.
For example, the code for our Event entity (in /src/main/scala/code/model/Event.scala): 

    class Event extends LongKeyedMapper[Event] 
      with IdPK {
        def getSingleton = Event 
        
        object eventName extends MappedString(this, 30) 
            with ValidateLength {
          override def validations = 
            valMinLen(3, "Event name must contain at least 3 characters.") _ ::
              super.validations
        }
        
    }
    
    object Event extends Event 
      with LongKeyedMetaMapper[Event] {}
      
I have added in a simple validation rule to help test our handling of form submission failures later.

## HTML
The HTML pages for manipulating each entity are derived from five Lift templates: 
For our Event entity they are:

 * createevent.html
 * listevent.html
 * editevent.html
 * viewevent.hmtl. 
 * deleteevent.hmtl. 
 
For each entity, the templates reside in their own sub-directory of src/main/webapp.
E.g. for Event, they are in: src/main/webapp/event.

Access to these pages are defined in the SiteMap, in Boot.scala:

        Menu("Create Event") /  "event" / "createevent",
        Menu("List Events") /  "event" / "listevent",
        Menu("Edit Event") /  "event" / "editevent" >> Hidden ,
        Menu("View Event") /  "event" / "viewevent" >> Hidden,
        Menu("Delete Event") /  "event" / "deletevent" >> Hidden,
 
editevent, viewevent, deleteevent are hidden menu items because access to 
these pages is via links on other pages. 

Note that there is a redundant 'event' in the path. One could have just had /event/create, /event/edit
However, having 'event' repeated in the filename (i.e. createevent) allows you to easily distinguish between 
multiple open files in an IDE such as Ecliplse.

The contents of each of the HTML templates is given below, adjacent to the corresponding snippet.

## Snippet
Each entity has an 'Ops' (short for CRUD Operations) snippet class.

The Ops snippet class for each entity lives in its own file, which lives in the snippet package. 

For our Event entity this is src/main/scala/code/snippet/EventOps.scala. 

This class contains a single render method for each HTML template; the general structure being:

      class EventOps {
        def create = { ... }
        def edit = { ... }
        def list = { ... }
        def view = { ... }
        def delete = { ... }
       }
       
## The form processing lifecyle
It's worth taking a moment to recap. on Lift's form processing lifecyle. 

When the user types a URL of a form into a browser, say http://localhost:8080/event/createevent
the browser first makes a HTTP GET request to the server for the specified resource. 

Lift returns a HTML form to the browser with the 'action' attribute set to be the URL the user has just requested.

    <form action="/event/createevent" method="post">
     ...
    </form>

When the form is then submitted,
the browser makes a HTTP PUT request to this same URL, sending the form data in the body of the request. 

As Lift developers we have to decide what we return to the user in response to this PUT.

Within the context of this approach, we use the following convention: 

  * If the form processing fails (e.g. the name field contains less than three characters)
we return the same form HTML again, but this time with the 
input fields populated with the contents that the user has just submitted. 
If the user clicks submit again the browser makes another PUT request, and so the
cycle continues.
  * On the other hand, if the form processing succeeds, 
we redirect the browser to the
next page in the work-flow, which, in our case we choose the list events page.

Thus, every time a form is processed the snippet behind the form is called at least twice; 
once for the initial GET request and once more for every subsequent PUT request.

## Managing State
Each 'Ops' snippet class has a RequestVar used to pass server-side 
state between successive HTML page requests. For the Event instance, this is declared as:

      object eventRV extends RequestVar[Event](Event.create)

 We use the RequestVar to pass Event instances between page request in the following cases:
 
 * from the initial GET request of a create or edit form to the subsequent 
 PUT request that processes the submitted data (and also between successive PUT requests if they
 occur)
 * from the list page to a delete, view or edit page
 * from a view page to an edit page
 
The RequestVar object is declared in a scope such that each 'Ops' class method has access to it. 
In our template we put it in class scope, but it could also have been declared at file scope; 
in some more complex use-cases file scope would be necessary to facilitate 
communication between multiple 'Ops' classes. 
I'll be giving examples of such use-cases in a future blog post.

For a more detailed discussion of RequestVars, see another of my other blog post:
[Understanding Lift's RequestVars](http://tech.damianhelme.com/understanding-lifts-requestvars)

## Create
Events are created through the createevent.html template:

    <div data-lift="lift:surround?with=default;at=content">
      <h2 class="alt">New Event</h2>
      <div data-lift="EventOps.create?form=post">
        <span id="hidden"></span>
        <table>
            <tr>
              <td>Event Name</td>
              <td><input id="eventname" type="text" /></td>
            </tr>
         </table>
        <input id="submit" type="submit" /> <br />
      </div>
    </div>

The corresponding snippet render method:

      def create = {
        var event = eventRV.is                             // 1
        "#hidden" #> SHtml.hidden(() => eventRV(event) ) & // 2
        "#eventname" #> SHtml.text(eventRV.is.eventName,   // 3a
                    name => eventRV.is.eventName(name) ) & // 3b
        "#submit" #> SHtml.onSubmitUnit(processSubmit)      // 4
      }
      
Most of this standard is Lift form / snippet processing that's described in detail 
in Simply Lift and Exploring Lift, so I won't give a complete commentary on the code here. 
However, the following points are worth emphasising:

When user makes the initial GET request on eventcreate.html, when line (1) is executed, eventRV.is is called for
the first time on this eventRV instance. 
As we have not yet initialised eventRV elsewhere, eventRV initialises itself
calling Event.create, the default function specified when we declared eventRV.

However, if create is being called on a PUT request, 
say for example after a form reload after validation has failed, 
eventRV.is returns the event that was previously set by the SHtml.hidden function at line (2). 

SHtml.hidden inserts a hidden input field into the HTML page and registers an associated 
server-side function <code>() => eventRV(event)</code> that Lift will call when the form is submitted.
In this case the registered function sets the eventRV in the subsequent PUT request to contain the event instance 
that was used used in the current request. 

For more information on the pattern used here to pass an instance from one page request to another
see [Understanding Lift's RequestVars](http://tech.damianhelme.com/understanding-lifts-requestvars)

Note that the SHtml.hidden line comes before the other HTML callback functions (SHtml.text, SHtml.onSubmit). 
This ordering is important since Lift calls the functions in the same order they are 
declared here. 
We want eventRV to be set before we start setting the member variables of Event instance contained in eventRV.

In our simple example, this SHtml.hidden line isn't strictly necessary. However it's useful to include as a general rule.
Consider what would happen if this line was omitted: on the subsequent createevent PUT request, 
when eventRV.is is called, eventRV would
not have been set and so a new Event instance would be created. 

In our example, this would be OK since the contents of name field would be written to the 
new instance (via the closure on line 3) when we submitted the form.
The instance used on the previous request be lost to garbage collection in the usual way.
So, the main advantage in this example of including the SHtml.hidden line 
would be that it prevents unnecessary garbage collection.

However there are some use cases where the SHtml.hidden line would be strictly necessary.
It may have been the case that the Event class had some fields that were not set via the form. 
For example, suppose that the Event class had member variable containing a foreign key to a
Location instance (representing the many-to-one relationship in real-life 
where events are held at a particular location). 
Suppose also that the 'createvent' page can only be invoked from 
within the context of specific location e.g. from a link embedded in the 'viewlocation' page, 
and when the event is created we want its location field to be set automatically.

The actual code to do this it bit too lengthy to give  here, but it would be possible
for the create event operation to know the location context from which it have been called and the 
EventOps.create function to set the Event location member accordingly. 

However, without the SHtml.hidden call in the EventOps.create, 
this location setting would be lost if the form submission failed and the create page 
was reloaded. 

To finish with a brief run through of the rest of this method, 
Line (3) uses a common pattern for binding the fields of a Mapper entity to a HTML form.
Line (4) registers a function that Lift will call to process the contents of the form
when it is submitted.

      def processSubmit() = {
        eventVar.is.validate match {
          case  Nil => {
            eventVar.is.save 
            S.notice("Event Saved")
            S.seeOther("/event/listevent")
          } 
          case errors => S.error(errors)
        }
        // exiting the function here causes the form to reload
     }
  
These are standard Lift techniques, for more information  
see, for example, (Simply Lift)[http://stable.simply.liftweb.net/#toc-Section-7.10]) .

If processSubmit succeeds, we're taken to the listevent.html page which in-turn invokes the EventOps.list method. 
Note that the SHtml.hidden method will still have set the eventRV, 
but as the list method does't use the eventRV, it will be silently ignored.

## Moving between different HTML page views on the same data
When we move between the list, view and edit pages, we use the event RequestVar to hold the Event instance in memory
on the server between subsequent page requests. 
Thus in the list view, when we click on a item's view link, 
the following view page will be
rendered using the same event instance in memory as was used to render that line in the list view. 
Similarly, from an event's view page, 
when when we click on the edit link on that page, the same in memory event instance is used to render the subsequent edit page.

The following sections describe this mechanism in more detail

## List

The listevent.html template:

    <div data-lift="lift:surround?with=default;at=content">
      <h2>Events</h2>
        <a href="/event/createevent">New Event</a> <br />
        <div data-lift="EventOps.list">
          <table>
            <thead>
              <tr>
                <th>Event Name</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody id="eventlist">
              <tr class="row">
                <td class="eventName">Dummy Name</td>
                <td class="actions">Actions</td>
              </tr>
            </tbody>
          </table>
        </div>
    </div>
    
The list snippet: 

      def list = {
        val allEvents = Event.findAll                             // 1
        ".row *" #> allEvents.map( t => {                         // 2
          ".eventName *" #> Text(t.eventName) &                   // 3
          ".actions *" #> {                                       // 4
              SHtml.link("/event/viewevent",                      // 5a
                () => eventRV(t), Text("view")) ++ Text(" ") ++  // 5b
              SHtml.link("/event/editevent",                      // 6a
                () => eventRV(t), Text("edit")) ++ Text(" ") ++  // 6b
              SHtml.link("/event/listevent",                      // 7a 
                () => {t.delete_!}, Text("delete"))}              // 7b
        } )          
      }
      
This EventOps.list method uses a common pattern for displaying a list of entities based on a html template 
For more information, see, for example, Binding To Children section of the Lift Wiki's 
[Binding Via CSS Selectors](http://www.assembla.com/spaces/liftweb/wiki/Binding_via_CSS_Selectors)).

In brief: at line (1) all the events are loaded from the database into a List held in memory. 
Lines (5),(6), & (7) render each event as a line in the table, with associated hyperlinks to the 'viewevent', 
'editevent' and 'deleteevent' pages.
Bound with the view and edit link is the function '() => eventRV(t)'. 

This function is called on the server when the link is clicked and sets the eventRV for the scope of the resulting 
page request to contain the event of the line which was clicked.

## Edit

The editevent.html:

    <div data-lift="lift:surround?with=default;at=content">
     <h2 class="alt">Edit Event</h2>
      <div data-lift="EventOps.edit?form=post">
       <span id="hidden"></span>
        <table>
            <tr><td>Event Name</td><td><input id="eventname" type="text" /></td></tr>
         </table>
         <input id="submit" type="submit /> <br />
      </div>
    </div>

The EventOps.edit snippet:

      def edit = {
        if ( ! eventRV.set_? )                              // 1
              S.redirectTo("/event/listevent")
        
        val event = eventRV.is                              // 2
       "#hidden" #> SHtml.hidden(() => eventRV(event) ) &   // 3
       "#eventname" #> SHtml.text(eventRV.is.eventName,     // 4a
                    name => eventRV.is.eventName(name) ) &  // 4b
       "#submit" #> SHtml.onSubmitUnit(processSubmit)       // 5
      }

This snippet is very similar to the 'create' snippet we discussed previously except with the 
following differences:

At line (1), we firstly make sure that the eventRV has been set. 
eventRV should have been set when the user clicks on the either of the edit links on the list or view pages.
Here, we're trapping case that the user has typed in the url page directly. 

The edit snippet has the same 'SHtml.hidden' mechanism for handling form submission failures, however in 
this case it is strictly required. The event instance has an 'id' member variable
corresponding to the primary key of the record in the database
(Event inherits this field from IdPK). The id field is assigned a value when save is first called on the instance. 
As we're editing an existing event, the id is non-null
and so on future calls to 'save', Mapper knows to update an existing record in the database rather than create a new one

If we didn't have the SHtml.hidden line, the subsequent PUT request would create a new event instance
which would have an uninitialised id, and so when saved to the database would result
in a second database instance of the event being created.

## View
The viewevent.html template:

    <div data-lift="lift:surround?with=default;at=content">
      <h2 class="alt">View Event</h2>
      <div data-lift="EventOps.view">
         <table>
            <tr><td>Event Name</td><td id="eventname">Dummy Name</td></tr>
         </table>
         <a id="edit" href=#>Edit</a>
      </div>
    </div>

The snippet code for the view operation is:

      def view = {
        if ( eventRV.set_? ) 
          S.redirectTo("/event/listevent")
          
        var event = eventRV.is
        "#eventname *" #> eventRV.is.eventName.asHtml &
        "#edit" #> SHtml.link("/event/editevent", () => eventRV(event), Text("edit"))
      }
      
This code is mostly similar to the other CRUD operations present so far.
The main difference now is that we're using MappedField's 'asHtml' method (eventName inherits from MappedField)
to render a read-only display of the name value.

## Delete
The deleteevent.html template:

    <div data-lift="lift:surround?with=default;at=content">
      <h2 class="alt">Delete Event</h2>
      <div data-lift="EventOps.delete">
      <p>Are you sure you want to delete event: <span id="eventname"></span>?</p>
         <a id="yes" href=#>Yes</a>
         <a id="no" href=#>No</a>
      </div>
    </div>
    
The EventOps.delete snippet:

    def delete = {
     if ( ! eventVar.set_? ) 
        S.redirectTo("/event/listevent")
        
     var e = eventVar.is
     "#eventname" #> eventVar.is.eventName &
     "#yes" #> SHtml.link("/event/listevent", () =>{ e.delete_!}, Text("Yes")) &
     "#no" #> SHtml.link("/event/listevent", () =>{ }, Text("No")) 
    }
    
The main technique to note here is that the deletion occurs by Lift calling the 
`() =>{ e.delete_!}` funtion registered with the 'Yes' link. 
This function forms a closure around the event instance that the user
is wanting to delete, and when executed calls the `Mapper.delete_!` function on that instance.

## Summary
Hopefully this has given you a strong enough starting point from which you can
start to build your own CRUD forms. Please feel free to copy and past the example
files given here

After using this technique a couple of times
I suspect you'll find it easy enough and quick enough for it to become your default
starting point for all future CRUD functionality.
 
Please leave comments, thoughts, questions etc. 

## Resources
  * [Simply Lift](http://stable.simply.liftweb.net/), David Pollak
  * [Exploring Lift](http://exploring.liftweb.net/master/index.html), Marius Danciu and Tyler Weir 
  * My blog post: [Understanding Lift's RequestVars](http://tech.damianhelme.com/understanding-lifts-requestvars)
