/*
 * ModifyFonts.java
 *
 * Created on December 3, 2007, 4:04 PM
 *
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import org.acorns.language.*;

/*
 *  This class processes commands that modify lesson header information.
 */
public class ModifyFonts extends MenuOption
{  static KeyboardFonts keyboards;
   
   /*
    * This is a polymorphic method that processes user commands
    *
    * @param status The Object controlling the Acorns environment
    * @param args   An array of user arguments needed to process this command
    */
   public String processOption(String[] args)
	  { 
	    JFontChooser.startDialog(KeyboardFonts.getLanguageFonts());
		KeyboardFonts newKeyboards = JFontChooser.getResult();
		if (newKeyboards==null) 
           return LanguageText.getMessage("acornsApplication", 70);
 
     KeyboardFonts.setLanguageFonts(newKeyboards);
     return LanguageText.getMessage("acornsApplication", 46);
     
	  } // End of processOption method.
}    // End of ModifyFonts class.


