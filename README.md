# Build your own CRUD screens in Lift

This is example code for building your own CRUD screens in Lift.

For a complete write-up, see the blog-post: TBC


To download, build and run:

        $ git clone git://github.com/dph01/lift-CRUDBasic.git
        $ cd lift-CRUDBasic
        $ ./sbt
        >container:start

To import the project into Eclipse, from within Sbt:

        >eclipse with-source=true
        
    Then from within Eclipse, File->Import -> Existing Projects into Workspace, and navigate to your lift-CRUDBasic directory.
    
As a bonus ;-) the project comes with a suite of Selenium-based tests. 
You can see 100% test coverage of the Event and EventOps class using sbt-scct 
from https://github.com/dvc94ch/sbt-scct.

## Running Version
To see a running version of this code, go to [www.damianhelme.com/crudbasic](www.damianhelme.com/crudbasic)

