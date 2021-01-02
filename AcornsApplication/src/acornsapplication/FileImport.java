/*
 * FileImport.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.io.*;
import org.acorns.*;
import org.acorns.audio.SoundDefaults;
import org.acorns.lesson.*;
import org.acorns.lib.*;
import org.acorns.language.*;

//----------------------------------------------------------
// Class to export a file with a selected name.
//----------------------------------------------------------
public class FileImport extends MenuOption
{
	//----------------------------------------------------------
	// Method to process import options.
	//----------------------------------------------------------
	public String processOption(String[] args)
	{
		String       fullName = null; // Name of selected import file name.
		AppObject   tempFile = null;  // The object of the temporarily open file.
		String      message = "";     // Possible error message.

		// Check if the maximum number of files are open.
		if (Files.isMaxFilesOpen())	
		{  return LanguageText.getMessage("acornsApplication", 36); }

		// Create a new file for opening.
		try
		{
			tempFile = new AppObject();
			Files.addOpenFile(tempFile);
			File file = null;

			// Create file chooser window.
			if (args == null)
			{
				String title =LanguageText.getMessage("acornsApplication", 38);
				int option = AcornsProperties.OPEN;
				
				final String[] exportArray = {"xml"};
				final DialogFilter dialog
						= new DialogFilter("Import Files", exportArray);

				file = AppEnv.chooseFile(title, option, dialog, null, null);
				
				if (file==null)
				{
					AppObject newFile 
							= (AppObject)Environment.getActiveFile();
					if (newFile!=null) 
					{  
						newFile.closeFile(false); 
						Files.removeOpenFile(newFile); 
					}
					return LanguageText.getMessage("acornsApplication", 45);
				}
				else
				{
					// Add the .xml suffix to the file name if needed.
					fullName   = file.getCanonicalPath();
					
		             if (!SoundDefaults.isValidForSandbox(fullName))
		            	 return LanguageText.getMessage("acornsApplication", 160);
		             
		             if (!fullName.endsWith(".xml"))
		             {
		                fullName = fullName + ".xml";
		                file     = new File(fullName);
		             }
				}   // End of if file approve option
			}
			else  
			{
				file = new File(args[0]);
				fullName = file.getCanonicalPath();
			}

			if (!file.exists()) 
				return LanguageText.getMessage("acornsApplication", 37) + ": "
				+ file.getName();

			// Verify that the file can be read.
			if (Files.isFileOpen(fullName)) 
			{    
				return LanguageText.getMessage("acornsApplication", 41)
						+ ": "+ fullName;
			}

			// Get the path to the file and the file name.
			String name = file.getName();
			int lastIndex = fullName.lastIndexOf(name);
			if (lastIndex<0) return "Error: Illegal File Name";
			String path = fullName.substring(0,lastIndex-1);

			// Save the path to the last open file.
			Environment.setPath(AcornsProperties.OPEN, path);

			// Finally we execute the import.
			String separator = System.getProperty("file.separator");
			message = tempFile.importFile(file, path);
			if (message==null)
				return LanguageText.getMessage("acornsApplication", 46)
						+ ": " + path + separator + name;
		}
		catch (IOException iox)   {}
		catch (NoDefaultNameException ndf) 
		{
			return LanguageText.getMessage("acornsApplication", 40);
		}

		// Handle import failures.
		if (tempFile==null)    
			return LanguageText.getMessage("acornsApplication", 39);

		boolean answer = tempFile.closeFile(false);
		if (answer) 
		{
			Files.removeOpenFile(tempFile);
			return message;
		}
		return LanguageText.getMessage("acornsApplication", 44) + " " + fullName;

	}  // End of processOption().
}     // End of FileImport.
