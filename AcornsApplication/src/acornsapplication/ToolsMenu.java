/*
 * ToolsMenu.java
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

public class ToolsMenu extends AcornMenu
{
   private final static long serialVersionUID = 1;
	
   // Constructor for the File menu bar.
   public ToolsMenu(JToolBar toolbar, JMenuBar bar)
   {
      super("Tools;"+LanguageText.getMessage("acornsApplication", 125), 't');

      String[] msgs = LanguageText.getMessageList("acornsApplication", 125);

      menuItem(toolbar, "Execute;"+msgs[1], 'x', "run.png", LESSON);
	  addSeparator();
      menuItem(toolbar, "Options;"+msgs[2], 'o', null, ALWAYS);
      menuItem(toolbar, "Audio;"+msgs[3], 'a', null, ALWAYS);
      menuItem(toolbar, "Feedback Recordings;"+msgs[4], 'f', null, ALWAYS);
      setEnableStatus();
      bar.add(this);
   }
 
   // Implement action listener for this menu.
   public void actionPerformed(ActionEvent ae)
   {  
	  processCommand(ae);
	  setEnableStatus();		
   }
}