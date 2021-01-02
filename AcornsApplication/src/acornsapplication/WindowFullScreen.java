/*
 * WindowFullScreen.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import org.acorns.language.*;

// Close the active file.
public class WindowFullScreen extends MenuOption
{
   public String processOption(String[] args)
	  {  
      Files.switchView(false);
		    return LanguageText.getMessage("acornsApplication", 120);
	  }
}