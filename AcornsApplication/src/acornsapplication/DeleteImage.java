/*
 * DeleteImage.java
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

// Open an existing file.
public class DeleteImage extends MenuOption
{
   public String processOption(String[] args)
	  {
      FileObject active;
      Lesson     lesson;

      try
      {
         if ((active = Files.getActiveFile()) == null)
         {  return LanguageText.getMessage("acornsApplication", 61); }

         if ((lesson = active.getActiveLesson()) == null)
         {  return LanguageText.getMessage("acornsApplication", 62); }

       lesson.removePicture(-1);
       active.displayLesson();
         return LanguageText.getMessage("acornsApplication", 63);
      }
      catch (Exception exception)
      {
         return LanguageText.getMessage("acornsApplication", 64);
      }
	  }
}

