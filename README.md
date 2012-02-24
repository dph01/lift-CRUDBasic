This is very simple framework for creating your own CRUD functions for a single database entity.

Lift has a few useful ways of generating CRUD operations automatically from your database classes i
(e.g. CRUDify, LiftScreen, Mapper.toForm etc). 
Thanks to thoughtful class design and useful Scala language features you can easily override much of the CRUD functionality
and layout to suit your particular needs. 

I've found these tools to be excellent for rapid prototyping, or even production code that isn't wanting to do anything too flashy.
However, in many cases, I've wanted to override so much of the default code that it made more sense to write my own CRUD code from the ground up.

I found this to be surprisingly tricky; I had to put a bit of thought into how best to orgnaise the code,
and there were a few subtle gotchas regarding the use of RequestVars that had me scratching me head for a while.

This post is written to help other's who may be treading the same ground; hopefully to help people navigate through more 
efficiently that I did.

This assumes familiarity with ....

# Setup

1. To download, build and run application:

        git clone git://github.com/dph01/lift-CRUDBasic.git
        cd lift-CRUDBasic
        ./sbt
        >container:start

2. To import the project into Eclipse, from within Sbt:

        >eclipse with-source=true
        
    Then from within Eclipse, File->Import -> Existing Projects into Workspace, and navigate to your lift-CRUDBasic directory.

# Running Version
To see a running version of this code, go to [www.damianhelme.com/CRUDBasic](www.damianhelme.com/CRUDBasic)

# Code orientation

# Overview
In this sample we're using a working with a single entity called 'Event'. It has just one attribute called EventName.

We have pages for the listing all events, viewing (i.e. read only) a single event, editing an existing event, 
and creating a new event.

The diagram below shows the allowable page transitions. We enter the screen flow through either the list or create
page. We can only navigate to the view and edit page through one of the other pages.

Note that for the sake of brevity I have not included a separate  page to prompt for confirmation when deleting
an entity, as is sometimes the case in other CRUD frameworks. In this case deletion happens without prompting.

[screen flow]

## Model
This example uses Lift's Mapper class to manage database access. The code for our event 
class is in /src/main/scala/code/Event.scala.

    class Event extends LongKeyedMapper[Event] 
      with IdPK {
        def getSingleton = Event 
        
        object eventName extends MappedString(this, 30) 
            with ValidateLength {
          override def validations = valMinLen(3, "Event name must contain more than 3 characters.") _ ::
              super.validations
        }
        
    }
    
    object Event extends Event 
      with LongKeyedMetaMapper[Event] {}

I have added in a simple validation rule to help test our handling of form submission failures later.

## HTML
For our event entity there four html templates. They are named: createevent.html, listevent.html, editevent.html, viewevent.hmtl. 
These are all grouped together in a common sub-directory of src/main/webapp/event.

Access to these pages are defined in Boot.scala:

        Menu("Create Event") /  "event" / "createevent",
        Menu("List Events") /  "event" / "listevent",
        Menu("Edit Event") /  "event" / "editevent" >> Hidden ,
        Menu("View Event") /  "event" / "viewevent" >> Hidden
 
editevent and viewevent are hidden in the Menu because access to these pages is intended to be via other pages. 

## Snippet
The event entity has an 'Ops' snippet class. There is a method in this class for each of the html templates. 
In this case, this file src/main/scala/code/snippet/EventOps.scala:

      class EventOps {
        def create = { ... }
        def edit = { ... }
        def list = { ... }
        def view = { ... }
       }

Each 'Ops' snippet class has a [RequestVar](http://stable.simply.liftweb.net/#toc-Section-4.4) to help manage state between successive page requests on this object. 

      object eventVar extends RequestVar[Event](null)

RequestVars are a mechanism that allow variables to be shared across different code segments on a per-request basis. 

There are two occasions where we need to manage state between successive page requests: 

* reloading data after a form submission (normally for when the submission fails and 
the problematic data is re-presented to the user for correction)
* moving between different HTML page views on the same data: e.g. from a 'view' to 'edit', or 'list' to 'view'

I'll describe how we deal with each of these cases in turn.

### Reloading data after a form submission
The first thing to note is that the eventVar is defined in a scope that is common to all the snippet methods. 
In this case I put it in the class scope, but it could also have been defined in file scope. In fact,
with more complex requirements there are good reasons to put the RequestVar at file scope, but that's
a topic for a future post.

Secondly, it's worth while to look more closely at the declaration of the eventVar object.

      object eventVar extends RequestVar[Event](Event.create)

On first impressions it may appear that eventVar is initialised with a new Event object returned from Event.create.
However, this is not the case. Looking at the signature of the RequestVar constructor we see that
that it actually taking a function that returns an Event instance rather than an Event instance. 
The RequestVar uses this
function to create a 'default' Event instance in the event that eventVar object is accessed before 
it has been set.

Thus, in the EventOps.create function:

     def create = {
        ...
        var event = eventVar.is
        ...
        
The first time the a user loads the 'createevent' page, this eventVar will not have been set, so calling eventVar.is
will trigger the Event.create method to be called to generate a new Event object. If the form submissions fails
the next time the form is reloaded this eventVar will have been pre-populated with the Event values
that caused the submission to fail. 

I'll step though the code in the create method to describe how this works:


      def create = {
        var event = eventVar.is                             // 1
        "#hidden" #> SHtml.hidden(() => eventVar(event) ) & // 2
        "#eventname" #> SHtml.text(eventVar.is.eventName,   // 3a
                    name => eventVar.is.eventName(name) ) & // 3b
        "#submit" #> SHtml.onSubmitUnit(processSubmit)      // 4
      }
      
Lines (3) and (4) are using a common pattern for binding a Mapper entity to a HTML form 
(described for example in more detail in
(Simply Lift)[http://stable.simply.liftweb.net/#toc-Section-7.10]).
When the page is loaded, the current value of eventVar.is.eventName is rendered on the page (4a). 
When the form is submitted eventVar.is.eventName is set to be the contents of the name field (4b), 
and the processSubmit function is called (5).

At line (2) another common pattern is used 
to pass the current value of the eventVar to a subsequent page load 
(see [Exploring Lift - 3.11 Session and Request State](http://exploring.liftweb.net/onepage/index.html) 
for a more detailed discussion of this technique).
Here a hidden html form element holds a reference to a server-side function. This function gets called when
the form is submitted and sets the eventVar for the scope of this new request. 
See the Exploring Lift reference above for details on why we initialise a local event var at (2) for use in the
this function.

So, if processSubmit fails to validate or save the eventVar it will result in the current (create) page being reloaded. 
In this case, the 'hidden' function will have set the eventVar to the value it contained that triggered the submission
failure. In that case, when line (1) is executed a second time, this time eventVar.is now doesn't call Event.create, but 
instead returns value that was previously set.

If processSubmit succeeds, we're taken to the listevent.html page which in-turn invokes the EventOps.list method. 
The hidden method will still have set the eventVar, but as the list method does't use the eventVar, it will be silently ignored.

### Moving between different HTML page views on the same data
When we move between the list, view and edit pages, we have the facility for passing the Event instance in memory
on the server between subsequent page requests. Thus in the list view, when we click on a item's view link, 
the following view page will be
rendered using the same event instance in memory as was used in the list view. Similarly, from an event's edit view, 
when when we click on the edit link, the same in memory event instance is used for both views.

The following sections describe this mechanism in more detail

#### List
This EventOps.list method uses a common pattern for displaying a list of entities based on a html template 
(see the Binding To Children section of the Lift Wiki's 
(Binding Via CSS Selectors](http://www.assembla.com/spaces/liftweb/wiki/Binding_via_CSS_Selectors)).

      def list = {
        val allEvents = Event.findAll                             // 1
        ".row *" #> allEvents.map( t => {                         // 2
          ".eventName *" #> Text(t.eventName) &                   // 3
          ".actions *" #> {                                       // 4
              SHtml.link("/event/viewevent",                      // 5a
                () => eventVar(t), Text("view")) ++ Text(" ") ++  // 5b
              SHtml.link("/event/editevent",                      // 6a
                () => eventVar(t), Text("edit")) ++ Text(" ") ++  // 6b
              SHtml.link("/event/listevent",                      // 7a 
                () => {t.delete_!}, Text("delete"))}              // 7b
        } )          
      }

At line (1) all the events are loaded from the database into a List held in memory. 
Lines (5),(6), & (7) render each event as a line in the table, which associated hyperlinks to the 'viewevent', 
'editevent' and 'deleteevent' pages.
Bound with the view and edit link is the function '() => eventVar(t)'. 
This function is called on the server when the link is clicked and sets the eventVar for the scope of the resulting 
page request; the eventVar is set to contain the event of the line which was clicked.

#### Edit
Thus, when for example the 'editevent' hyperlink is clicked, editevent html page is loaded and 
Lift invokes the EventOps.edit snippet:

      def edit = {
        if ( eventVar.set_? ) {
          val event = eventVar.is                              // 1
         "#hidden" #> SHtml.hidden(() => eventVar(event) ) &  // 2
         "#eventname" #> SHtml.text(eventVar.is.eventName,    // 3a
                      name => eventVar.is.eventName(name) ) & // 3b
         "#submit" #> SHtml.onSubmitUnit(processSubmit)       // 4
        } else {
         "*" #> "Navigation Error. Access the Edit page through the either the List or View page."
        }   
      }

This snippet is very similar to the 'create' snippet we discussed previously. 
Lines (3) and (4) render the contents of eventName on the form load and specify the functions to be
called when the form is submitted. 

In the code we also add a check to make sure that the eventVar has been set. This
traps the case that the user type in the url page directly rather then navigating to 
the page via the List or View page.

I've omitted this checking in this example for the sake of brevity. In the normal screen flow
we would arrive at the edit screen from either the list or view (more later) screens. In both of
these cases we know that the eventVar would be set from the function attached to the link. However,
there is still the possibility that the user types the /event/listevent url directly in the browser
and so we need to handle that case gracefully.

The edit snippet has the same 'hidden' mechanism for handling form submission failures. See the description of the create 
snippet for more details.

#### View
The view snippet is perhaps the simplest of them all:

      def view = {
        if ( eventVar.set_? ) {
          var event = eventVar.is
          "#eventname *" #> eventVar.is.eventName.asHtml &
          "#edit" #> SHtml.link("/event/editevent", () => eventVar(event), Text("edit"))
          "#submit" #> SHtml.onSubmitUnit(processSubmit)       // 4
        } else {
         "*" #> "Navigation Error. Access the View page through the List page."
        }
      }
      
The eventName value is rendered using MappedField's 'asHtml' method (eventName inherits from MappedField).
We also have a hyper-link that navigates us to the editevent page, which using the same mechanism as described previously
sets the eventVar for the subsequent page request.


For comments, questions, etc. please see the accompanying [blogpost](http://tech.damianhelme.com/twitter-bootstrap-navbar-dropdowns-and-lifts)


