/*
 * FilePrintPreview.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.io.*;
import org.w3c.dom.*;
import java.awt.print.*;
import org.acorns.language.*;

// Close the active file.
public class FilePrintPreview extends MenuOption
{
   public String processOption(String[] args)
   {
      AppObject active = Files.getActiveFile();
      String[] msgs = LanguageText.getMessageList("acornsApplication", 105);
		
      Document fileData;
      try
      {
         fileData = active.print();

         String[] properties = active.getProperties();
         String title = properties[AppObject.PATH]
            + System.getProperty("file.separator")
            + properties[AppObject.NAME];

         AcornPrintable printableFile = new AcornPrintable(fileData, title);
	     new PrintPreview(printableFile);
     }
     catch (PrinterException ex)	{ return msgs[0]; }
     catch (IOException ex)    	 { return msgs[2]; }
     catch (Exception ex)        { return ex.toString(); }
     return msgs[1];
   }
}