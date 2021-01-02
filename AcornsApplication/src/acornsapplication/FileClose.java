/*
 * FileClose.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import org.acorns.*;
import org.acorns.language.*;

// Close the active file.
public class FileClose extends MenuOption
{
   public String processOption(String[] args)
	  {  
	     AppObject active = Files.getActiveFile();
	
		  String[] properties = active.getProperties();
		  String separator = System.getProperty("file.separator");
		  String path      = properties[FileObject.PATH];
		  String name      = properties[FileObject.NAME];
		  String fileName  = path + separator + name;
			
	      boolean answer = active.closeFile(true);
		   if (answer) 
		    {
		       Files.removeOpenFile(active);
		       return LanguageText.getMessage("acornsApplication", 89, fileName);
		    }
	     return LanguageText.getMessage("acornsApplication", 70);
	  }
}