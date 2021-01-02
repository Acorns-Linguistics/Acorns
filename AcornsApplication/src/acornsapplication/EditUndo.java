/*
 * EditUndo.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.language.*;

// Close the active file.
public class EditUndo extends MenuOption
{
   public String processOption(String[] args)
	  {  
	     FileObject active = Files.getActiveFile();
	  	  if (active == null) 
          return LanguageText.getMessage("acornsApplication", 61);

      Lesson lesson = active.getActiveLesson();
		    if (lesson==null) return LanguageText.getMessage("acornsApplication", 62);
		
		    if (lesson.undoCommand())
      {     active.displayLesson();
			         return LanguageText.getMessage("acornsApplication", 63);
      }
		    else  return LanguageText.getMessage("acornsApplication", 74);
	  }
}