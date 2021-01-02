/*
 * FileNew.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.io.*;
import org.acorns.*;
import org.acorns.language.*;

public class FileNew extends MenuOption
{
      public String processOption(String[] args)
	  {  
		    if (Files.isMaxFilesOpen())	
		    {  return LanguageText.getMessage("acornsApplication", 36);	}
		
		    try
		    {
		        AppObject file  = new AppObject();
			    Files.addOpenFile(file);
			
		        String[] properties = file.getProperties();
		        String path = properties[FileObject.PATH];
		        String name = properties[FileObject.NAME];	
		        
			    String separator = System.getProperty("file.separator");
		        return LanguageText.getMessage
                    ("acornsApplication", 93, path + separator + name);
		    }
		    catch (NoDefaultNameException dfne)	
                   {return LanguageText.getMessage("acornsApplication", 40);}
		    catch (IOException iox)	{return iox.toString();}
	  }
}
