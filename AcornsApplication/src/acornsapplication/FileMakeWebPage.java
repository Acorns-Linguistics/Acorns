
 /* FileMakeWebPage.java
 *
 * Created on December 6, 2007, 4:17 PM
 *
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 *   
 *   Note: merged with the make mobile application because
 *   Java applets present security problems and are no
 *   longer considered best practice.
 *   
 *   The APPLET version is commented out below; the two
 *   statements transfer to the make mobile application
 *   module.
 */
package acornsapplication;

//----------------------------------------------------------
// Class to export a file with a selected name.
//----------------------------------------------------------
public class FileMakeWebPage extends MenuOption
{
	//----------------------------------------------------------
	// Method to process export options.
	//----------------------------------------------------------
   public String processOption(String[] args)
   {
	  FileMakeMobileApp mobileApp = new FileMakeMobileApp();
	  return mobileApp.processOption(args);
   }  // End of processOption().
}      // end of FileWebPage

