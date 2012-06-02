/*
 * Copyright 2012 Damian Helme

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   
   This code is derived from the template: https://github.com/lift/lift_24_sbt
 */
package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import mapper._
import java.io.{File,FileInputStream}
import net.liftweb.http.provider.servlet.HTTPServletContext

import code.model._


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Logger {
  def boot {
 // when running in jetty, the following codes makes Lift look for a props file in
    // $JETTY_HOME/resources/<context path>/ before looking in the usual places allowing
    // you to edit props file without having to recompile the war
    // start Jetty with: java -Djetty.resources=$JETTY_HOME/resources -jar start.jar
    val contextPath = LiftRules.context match { 
      case c: HTTPServletContext => Full(c.path)
      case _ => Empty
    } 
    info("Context Path is: " + contextPath )

    val jettyResourcesDir = Box.!!(System.getProperty("jetty.resources"))
    info("got jetty.resources from system properties: " + jettyResourcesDir)
    val whereToLook = jettyResourcesDir.flatMap( dir => 
      contextPath.map( cp => 
      for ( 
            propsname <- Props.toTry;
            fullname = dir + cp + propsname() + "props";
            file = new File(fullname);
            if (file.exists) 
          ) yield fullname -> { () => Full(new FileInputStream(file))}
        )
    )
          
    info("adding the following on the Props.whereToLook: " + whereToLook.map(x => x.map( p => p._1)))
    
    whereToLook.foreach( w => Props.whereToLook = () => w )
    
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = 
           new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
			     Props.get("db.url") openOr 
			     "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
			     Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you want
    Schemifier.schemify(true, Schemifier.infoF _, Event)

    // where to search snippet
    LiftRules.addToPackages("code")

    // Build SiteMap
    def sitemap = SiteMap(
        Menu.i("Home") / "index", 
        Menu("Create Event") /  "event" / "createevent",
        Menu("List Events") /  "event" / "listevent",
        Menu("Edit Event") /  "event" / "editevent" >> Hidden ,
        Menu("View Event") /  "event" / "viewevent" >> Hidden,
        Menu("Delete Event") /  "event" / "deleteevent" >> Hidden
	       )

    // def sitemapMutators = User.sitemapMutator
    
    LiftRules.statelessReqTest.append {
      case StatelessReqTest("stateless" :: _, req) => true
    }
      
    LiftRules.setSiteMap(sitemap)


    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQuery14Artifacts

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    // LiftRules.loggedInTest = Full(() => User.loggedIn_?)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    

    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)
    
   // insert Google Analytics Tracking code into head of your html
    // Thanks for Richard Dalloway for the following code 
    // Copied and pasted from https://github.com/d6y/liftmodules-googleanalytics
    import scala.xml.Unparsed
    def headJs(id: String) = <script type="text/javascript">
      var _gaq = _gaq || [];
      _gaq.push(['_setAccount', '{Unparsed(id)}']);
      _gaq.push(['_trackPageview']);
      (function() {{
        var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
        ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
        var s = document.getElementsByTagName('script')[0]; 
        s.parentNode.insertBefore(ga, s);
      }})();
      </script>
    
    
    Props.get("google.analytics.id") map headJs foreach { js =>
      def addTracking(s: LiftSession, r: Req) : Unit = S.putInHead(js)
      
    LiftSession.onBeginServicing = addTracking _ :: LiftSession.onBeginServicing
  }
  }
}
