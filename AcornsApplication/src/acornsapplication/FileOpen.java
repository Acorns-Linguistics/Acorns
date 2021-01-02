/*
 * FileOpen.java
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
import org.acorns.lib.*;
import org.acorns.language.*;


// Open an existing file.
public class FileOpen extends MenuOption
{
   public String processOption(String[] args)
   {
     File file = null;

     try
     {  
        if (Files.isMaxFilesOpen())	
        {  return LanguageText.getMessage("acornsApplication", 36);	}

        if (args == null) 
        {
        	String title = LanguageText.getMessage("acornsApplication",  90);
        	int option = AcornsProperties.OPEN;
            DialogFilter dialogFilter = new DialogFilter();
            file = AppEnv.chooseFile(title, option, dialogFilter, null, null);
 
            if (file != null) 
            {
               if (!file.exists()) 
                    return LanguageText.getMessage
                                        ("acornsApplication", 71, file.getName());
            }
            else  return LanguageText.getMessage("acornsApplication", 70);
        }
        else  
        {  
           // Selection of a recently opened file.
           file = new File(args[0]);
        }

        AppEnv.addRecentFile(file.getCanonicalPath());
        return openFile(file);
     }
     catch (IOException iox)	
     {  String message = iox.getMessage();
        if (message.length()>0) return message;
        return LanguageText.getMessage("acornsApplication", 12);
     }
     catch (InvalidFileTypeException exception)
     {  return LanguageText.getMessage("acornsApplication", 12); }
     catch (Exception ex) { return ex.toString(); }
}

   /** Open the selected file 
    *  @param file object for the file to be opened
    *  @string message determining if successful (Starts with 'Error' if no).
    */
   public static String openFile(File file) throws Exception
   {
       // Verify that the file is not already open.
       String path = file.getCanonicalPath();
       if (Files.isFileOpen(path)) 
       {
         return LanguageText.getMessage("acornsApplication",91, file.getName());
       }

       // Get the path to the file and the file name.
       String name = file.getName();
       int lastIndex = path.lastIndexOf(name);
       if (lastIndex<0) 
            return LanguageText.getMessage("acornsApplication", 69, name);
       
       path = path.substring(0,lastIndex-1);

       // Finally we can open the file.
       AppObject fileObject = new AppObject(path, name);
       Files.addOpenFile(fileObject);
       String separator = System.getProperty("file.separator");
       AppEnv.setPath(AcornsProperties.OPEN, path);
       return LanguageText.getMessage
                        ("acornsApplication", 92, path + separator + name);
   }
}

