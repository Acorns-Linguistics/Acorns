/*
 * DeleteFile.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.io.*;
import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.language.*;
import org.acorns.lib.*;

// Open an existing file.
public class DeleteFile extends MenuOption
{
  public String processOption(String[] args)
  {
     File file = null;
            String fullName = "";

     // Standard open file option.
     int option = AcornsProperties.OPEN;
     String title = LanguageText.getMessage("acornsApplication", 65);
     final DialogFilter dialog = null;
            
     file = AppEnv.chooseFile(title, option, dialog, null, null);
     if (file!=null)
     {
        if (!file.exists()) 
        	return LanguageText.getMessage("acornsApplication", 71, file.getName());
     }
     else  return LanguageText.getMessage("acornsApplication", 70);

     // Verify that the file is not already open.
     try
     {
         String path = file.getCanonicalPath();
         if (Files.isFileOpen(path)) 
         {
            return LanguageText.getMessage
                    ("acornsApplication", 68, file.getName());
         }

         // Get the path to the file and the file name.
         String name = file.getName();
         int lastIndex = path.lastIndexOf(name);
         if (lastIndex<0) return LanguageText.getMessage
                                ("acornsApplication", 69, file.getName());
         path = path.substring(0,lastIndex-1);

         Environment.setPath(AcornsProperties.OPEN, path);

         // Finally we can delete the file.
         fullName = file.getCanonicalPath();
         if (file.exists())
         {  if (file.delete()) return LanguageText.getMessage
                          ("acornsApplication", 67, fullName);
         }
     }
     catch (IOException iox) {}
     return LanguageText.getMessage("acornsApplication", 72, file.getName());
  }     // End of process Option.
}        // End of DeleteFile class.

