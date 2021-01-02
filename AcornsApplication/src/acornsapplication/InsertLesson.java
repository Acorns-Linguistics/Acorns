/*
 * InsertLesson.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import org.acorns.*;
import org.acorns.language.*;

public class InsertLesson extends MenuOption
{
    public String processOption(String[] args)
    { 
      FileObject active = Files.getActiveFile();
      try
      {  
         if (active.newLesson(null, null))
	      return LanguageText.getMessage("acornsApplication", 78);
         else return LanguageText.getMessage("acornsApplication", 79);
      }
      catch (Exception exception)
      { return LanguageText.getMessage("acornsApplication", 70); } }
}
