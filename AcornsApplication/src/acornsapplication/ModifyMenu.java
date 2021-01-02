/*
 * ModifyMenu.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.event.*;
import javax.swing.*;
import org.acorns.language.*;

public class ModifyMenu extends AcornMenu
{

   private static final long serialVersionUID = 1;
	
   JMenuItem lesson, image, shape, sound;
	  
   // Constructor for the File menu bar.
   public ModifyMenu(JToolBar toolbar, JMenu menu)
   {
      super("Modify;"+LanguageText.getMessage("acornsApplication", 139), 'i');
      String[] msgs = LanguageText.getMessageList("acornsApplication", 139);

      lesson  = menuItem(toolbar, "Lesson;"+msgs[1], 'l', null, LESSON);
      menu.add(this);
      menuItem(toolbar, "Fonts;"+msgs[2], 'F', null, ALWAYS);
      setEnableStatus();
   }
 
   // Implement action listener for this menu.
   public void actionPerformed(ActionEvent ae)
   {  
	   processCommand(ae);
	   setEnableStatus();		
   }
}
