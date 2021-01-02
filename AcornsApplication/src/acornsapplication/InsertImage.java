/*
 * InsertImage.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.io.*;
import java.net.*;

import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.language.*;

// Open an existing file.
public class InsertImage extends MenuOption
{
   public String processOption(String[] args)
   {
       FileObject active;
       Lesson     lesson;

       try
       {  active = Files.getActiveFile();
	  lesson = active.getActiveLesson();

           String[] msgs = LanguageText.getMessageList("acornsApplication", 118);
           URL url;
           if (args!=null) { url = new File(args[0]).toURI().toURL(); }
           else
           {
              url = Environment.getEnvironment().getPicture();
              if (url==null) return msgs[0];
           }
			      
           lesson.insertPicture(url, 100,  0);
	       active.displayLesson();
	       return msgs[1];
       }
       catch (Exception exception)  { return exception.toString();	 }
   }
}     // End of InsertImage class
