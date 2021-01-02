/*
 * InsertMenu.java
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

public class InsertMenu extends AcornMenu
{

   private static final long serialVersionUID = 1;
	
   JMenuItem lesson, image, shape, sound;
	  
   // Constructor for the File menu bar.
   public InsertMenu(JToolBar toolbar, JMenu menu)
   {
      super("Insert;"+LanguageText.getMessage("acornsApplication", 116), 'i');

      String[] msgs = LanguageText.getMessageList("acornsApplication", 116);
		    lesson  = menuItem(toolbar, "Lesson;"+msgs[1], 'l', "lesson.png", OPEN);
      setShortcut(lesson, KeyEvent.VK_L);
      image = menuItem(toolbar, "Image;"+msgs[2], 'i', "image.png", LESSON);
      setShortcut(image, KeyEvent.VK_I);
      menu.add(this);
	  setEnableStatus();
   }
 
   // Implement action listener for this menu.
   public void actionPerformed(ActionEvent ae)
   {  
	    processCommand(ae);
		setEnableStatus();
   }
}
