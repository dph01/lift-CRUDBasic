# Build your own CRUD screens in Lift

This is example code for building your own CRUD screens in Lift.

For a complete write-up, see the blog-post: TBC

To download, build and run:

        $ git clone git://github.com/dph01/lift-CRUDBasic.git
        $ cd lift-CRUDBasic
        $ ./sbt
        >container:start

In a browser, go to http://localhost:8080 to see the app runing.

To import the project into Eclipse, from within Sbt:

        >eclipse with-source=true
        
Then from within Eclipse, File->Import -> Existing Projects into Workspace, and navigate to your lift-CRUDBasic directory.
    
As a bonus ;-) the project comes with a suite of Selenium-based tests. To run the tests, from within Sbt:

        > test
        
You can see 100% test coverage of the Event and EventOps class using sbt-scct. 

  * Download and publish locally sbt-scct from https://github.com/dvc94ch/sbt-scct.
  * Uncomment the line `addSbtPlugin("ch.craven" %% "scct-plugin" % "0.2.1")` from projects/plugins.sbt
  * Uncomment the line `Seq(ScctPlugin.scctSettings: _*)` from build.sbt
  * From within Sbt, generate the coverage report with: `>coverage:doc`
  * View the report at target/scala-2.9.1/coverage-report/coverage-report/index.html

## Running Version
To see a running version of this code, go to [www.damianhelme.com/crudbasic](http://www.damianhelme.com/crudbasic)

