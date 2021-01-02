/*
 * WindowTabbedView.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import org.acorns.language.*;

// Switched to the tabbed view of the open files.
public class WindowTabbedView extends MenuOption
{
   public String processOption(String[] args)
	  {  
      Files.switchView(true);
		    return LanguageText.getMessage("acornsApplication", 119);
	  }
}