package code.model
import net.liftweb.mapper._

class Event extends LongKeyedMapper[Event] 
    with IdPK {
    def getSingleton = Event 
    
    object eventName extends MappedString(this, 30)
    
}

object Event extends Event 
  with LongKeyedMetaMapper[Event] {}


