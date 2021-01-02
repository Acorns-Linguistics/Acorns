/*
 * EditCopyLesson.java
 *
 *   @author  HarveyD
 *   @version 6.00
 *
 *   Copyright 2010, all rights reserved
 */
package acornsapplication;

import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.language.*;

public class EditCopyLesson extends MenuOption
{  public String processOption(String[] args)
	{  try
	   {
         FileObject active = Files.getActiveFile();
         Lesson.copyLesson(active.getActiveLesson());
         return LanguageText.getMessage("acornsApplication", 83);
      }
      catch (Exception exception)   {  return exception.toString(); }
	}
}
