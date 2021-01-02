/*
 * WindowMenu.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.event.*;
import javax.swing.*;
import org.acorns.*;
import org.acorns.language.*;

public class WindowMenu extends AcornMenu
{
   private static final long serialVersionUID = 1;

   private String[] msgs;
	
   // Constructor for the File menu bar.
   public WindowMenu(JToolBar toolbar, JMenuBar bar)
   {
      super("Window;"+LanguageText.getMessage("acornsApplication",121), 'w');

      msgs = LanguageText.getMessageList("acornsApplication",121);
      
      menuItem(toolbar, "Full Screen;"+msgs[1], 'f', null, OPEN);
      menuItem(toolbar, "Tabbed View;"+msgs[2], 't', null, OPEN);
      addSeparator();
		
		    // Create array of menu items for any open files.
		    JMenuItem[] windows = new JMenuItem[Files.MAX_FILES];
		
		    for (int i=0; i<Files.MAX_FILES; i++)
		    {  
		        windows[i]
          = 	menuItem(toolbar,""+(i+1),Character.forDigit(i+1,10), null, ALWAYS);
			      windows[i].setVisible(false);
		    }
		    Files.putWindowItems(windows);
		    setEnableStatus();
		    bar.add(this);
   }
 
   // Implement action listener for this menu.
   public void actionPerformed(ActionEvent ae)
   {  
	  String result  = processCommand(ae);
      if (result.length()<2)
      {
          JMenuItem item = (JMenuItem)ae.getSource();
          result = item.getText();
      }
		    String file  = result.substring(2);
		
		    boolean type = Character.isDigit(result.charAt(0));
		    if (type)
		    {
		       boolean ok = Files.switchWindow(file);
			      if (ok) result = msgs[3] + " " + file;
			      else    result = msgs[4] + " " + file;
			      Environment.setText(result);
			      setEnableStatus();
		    }
   }
}