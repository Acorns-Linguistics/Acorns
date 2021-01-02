/*
 * DeleteMenu.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.event.*;
import javax.swing.*;
import org.acorns.language.*;

public class DeleteMenu extends AcornMenu
{
   private static final long serialVersionUID = 1;
	
   JMenuItem file, lesson, image;
	  
   // Constructor for the File menu bar.
   public DeleteMenu(JToolBar toolbar, JMenu menu)
   {
      super("Delete;" + LanguageText.getMessage("acornsApplication", 60), 'f');

      String[] msg = LanguageText.getMessageList("acornsApplication", 60);
	     file    = menuItem(toolbar, "File;"+msg[1],   'f', null,  ALWAYS);
		    lesson  = menuItem(toolbar, "Lesson;"+msg[2], 'l', null,  LESSON);
   	  image   = menuItem(toolbar, "Image;"+msg[3],  'i',  null, LESSON);
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
