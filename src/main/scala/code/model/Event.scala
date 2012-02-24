package code.model
import net.liftweb.mapper._
import net.liftweb.util.TimeHelpers
import net.liftweb.common.Logger

class Event extends LongKeyedMapper[Event] 
    with IdPK with Logger {
    def getSingleton = Event 
    
    object eventName extends MappedString(this, 30) with ValidateLength {
      this( TimeHelpers.millis.toString )
      debug("in Event.eventName constructor: " + this.is)
      override def validations = valMinLen(3, "Event name must be more than 3 characters.") _ ::
          super.validations
    }
    
}

object Event extends Event 
  with LongKeyedMetaMapper[Event] {}


