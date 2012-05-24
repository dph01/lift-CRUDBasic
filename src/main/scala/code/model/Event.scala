package code.model

import net.liftweb.common.Logger
import net.liftweb.mapper.{MappedString, ValidateLength, LongKeyedMetaMapper, LongKeyedMapper, IdPK}

class Event extends LongKeyedMapper[Event] 
    with IdPK with Logger {
    def getSingleton = Event 
    
    object eventName extends MappedString(this, 30) with ValidateLength {
      override def validations = valMinLen(3, "Event name must be more than 3 characters.") _ ::
          super.validations
    }
}

object Event extends Event 
  with LongKeyedMetaMapper[Event] {}


