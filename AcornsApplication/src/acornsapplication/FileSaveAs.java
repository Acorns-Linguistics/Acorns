/*
 * FileSaveAs.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.Frame;
import java.io.*;
import javax.swing.*;
import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.lib.*;
import org.acorns.language.*;

// Close an open file and give it a new name.
public class FileSaveAs extends MenuOption
{
   public String processOption(String[] args)
	{
	   try
	   {  
	        AppObject active = Files.getActiveFile();
			
	        String[] properties = active.getProperties();
            properties[FileObject.NAME] = active.getDefaultName();

            String path = AppEnv.getPath(AcornsProperties.SAVE);
		    String separator  = System.getProperty("file.separator");
		    String fullName   = path + separator + properties[FileObject.NAME];
			File   file       = new File(fullName);
			
		    // Standard save file option.
		    final int option = AcornsProperties.SAVE;
		    final String title = LanguageText.getMessage("acornsApplication", 85);
   		    final DialogFilter dialog = new DialogFilter();
   		    String defaultName = file.getAbsolutePath();
		    file = AppEnv.chooseFile(title, option, dialog, defaultName, null);

			if (file != null)
			{
			    // Add the .lnx suffix to the file name if needed.
		        fullName      = file.getCanonicalPath();
	            if (!fullName.endsWith(".lnx"))
	            {
	                fullName = fullName + ".lnx";
	                file     = new File(fullName);
	            }
	            
		        // Verify that the file is not already open.
			    FileObject fileObject = Files.findOpenFile(fullName);
			    if (fileObject!=null && fileObject != active) 
			    {
			       return LanguageText.getMessage("acornsApplication",68,fullName);
			    }
			
                // Verify that it is ok to replace an existing file.
                if (file.exists() && fileObject != active)
                {
       	           Frame frame = Environment.getRootFrame();
                   int answer = JOptionPane.showConfirmDialog(frame,
                   LanguageText.getMessage("acornsApplication", 87, fullName)
                          , LanguageText.getMessage("acornsApplication", 86)
                          , JOptionPane.OK_OPTION);

                   if (answer != JOptionPane.OK_OPTION)
                       return LanguageText.getMessage("acornsApplication", 70);

                   file.delete();
                   String backName
                       = fullName.substring(0, fullName.length()-4) + ".bak";
                   File backF = new File(backName);
				   if (backF.exists()) backF.delete();
				   String oldBakName 
                       = fullName.substring(0, fullName.length()-4) + ".bk2";
                   File oldBakF = new File(oldBakName);
                   if (oldBakF.exists()) oldBakF.delete();
				}
			}     // End if Approved
			else  return LanguageText.getMessage("acornsApplication", 70);
		
			// Get the path to the file and the file name.
			String name = file.getName();
		    int lastIndex = fullName.lastIndexOf(name);
		    if (lastIndex<0) 
               return LanguageText.getMessage("acornsApplication", 69, fullName);
		    
			path = fullName.substring(0,lastIndex-1);
			
			// Finally we can save the file.
			active.saveFile(path, name);
			Files.updateWindowMenu();
			AppEnv.setPath(AcornsProperties.SAVE, path);
		    return LanguageText.getMessage("acornsApplication", 88, fullName);
		}
		catch (IOException iox)	  
        { 
			return LanguageText.getMessage("acornsApplication", 12); 
	    }
   }  // End process()
}
