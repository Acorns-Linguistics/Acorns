/*
 * FilePrint.java
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
public class FilePrint extends MenuOption
{
   public String processOption(String[] args)
  	{  
	     AppObject active = Files.getActiveFile();
	     String[] msgs = LanguageText.getMessageList("acornsApplication", 105);

		 Document fileData;
		 try
	     {
		       PrinterJob job = PrinterJob.getPrinterJob();
    		   fileData = active.print();

    		   String[] properties = active.getProperties();
		       job.setJobName(msgs[5] + " " + properties[AppObject.NAME]);
    		   String title = properties[AppObject.PATH]
                        + System.getProperty("file.separator")
                        + properties[AppObject.NAME];

			      AcornPrintable printableFile = new AcornPrintable(fileData, title);
			      job.setPrintable(printableFile);//, format);
			      if (!job.printDialog())
			      {
			         return msgs[4];
   		    }
		       job.print();
		    }
		    catch (PrinterException exception)	{ return msgs[0]; }
		    catch (IOException exception)    	 { return msgs[2]; }
		    catch (Exception exception)        { return exception.toString(); }
	        return msgs[3];
	  }
}