/*
 * FileSave.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.io.*;
import org.acorns.*;
import org.acorns.language.*;

public class FileSave extends MenuOption
{
   public String processOption(String[] args)
	  {
        AppObject active = Files.getActiveFile();

        // Switch option to save as if file was never saved before.
        if (active.isDefaultName())
        {
           try
           {
              Class<?> className = Class.forName("acornsapplication.FileSaveAs");
              MenuOption classInstance = (MenuOption)className.getDeclaredConstructor().newInstance();
              return classInstance.processOption(args);
           }
           catch (Throwable t)	{return t.toString();}
        }

        // Process normal save operation.
        try
        {
           String[] properties = active.getProperties();
           String separator = System.getProperty("file.separator");
           String path      = properties[FileObject.PATH];
           String name      = properties[FileObject.NAME];
           String fileName  = path + separator + name;

           active.saveFile(path, name);
           return LanguageText.getMessage("acornsApplication", 88, fileName);
        }
        catch (IOException iox)	{ return iox.toString();		}
	  }
}
