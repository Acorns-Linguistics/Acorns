/*
 * DeleteLesson.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;
        
import java.io.*;
import javax.swing.*;
import org.acorns.language.*;

/**
 * Class that processes commands to delete lessons from file objects
 */
public class DeleteLesson extends MenuOption
{
   /**
    * <p>Polymorphic method that processes commands to delete lessons from a file</p>
    *
    * @param args Arguments needed to process this command
    */
    // Method called polymorphically from AcornMenu.
    public String processOption(String[] args)
	   {
		      AppObject active = Files.getActiveFile();
		      if (active == null) 
            return LanguageText.getMessage("acornsApplication", 61);

		      JPanel lessonInfo = active.getLessonInfo();
		      if (lessonInfo == null) 
            return LanguageText.getMessage("acornsApplication", 62);

       // Remove the active lesson.
		     try
		     {
		        active.removeLesson();
 		       return LanguageText.getMessage("acornsApplication", 63);
		     }
		     catch (IOException exception)
		     {
		         return LanguageText.getMessage("acornsApplication", 64);
		     }
	   }  // End of processOption
}
