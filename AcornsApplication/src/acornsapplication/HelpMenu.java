/*
 * HelpMenu.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.help.*;
import org.acorns.*;
import org.acorns.language.*;

public class HelpMenu extends AcornMenu
{ private final static long serialVersionUID = 1;
	
  private HelpSet        helpSet;
  private HelpBroker     helpBroker;
  private String[]       msgs;
	
  // Constructor for the File menu bar.
  public HelpMenu(JToolBar toolbar, JMenuBar bar)
  {
      super("Help;"+LanguageText.getMessage("acornsApplication", 117), 'h');

      // Table of contents button.
      msgs = LanguageText.getMessageList("acornsApplication", 117);
      String menuName = "Help Topics;"+msgs[1];
      JMenuItem helpAcorn = menuItem(toolbar, menuName,'t', "help.png", ALWAYS);
      helpAcorn.setAccelerator(KeyStroke.getKeyStroke("F1"));
      // Attach help to the table of contents button.
      JButton helpAcornButton = getButton(toolbar, msgs[1]);
      try  { makeListener(helpAcornButton, helpAcorn);  }
      catch (Throwable t)
      {  
          helpAcornButton.addActionListener(
                    new ActionListener()
                    {  public void actionPerformed(ActionEvent event)
                       {  JOptionPane.showMessageDialog
                                  (null, msgs[2]);
                       }
                    }
                 );
      }

      menuItem(toolbar,"About;"+msgs[3],'a',"blank.png",ALWAYS);
      bar.add(this);
  }
	
  // Get button from the toolbar.
  public final JButton getButton(JToolBar toolbar, String toolTip)
  {
     JButton button;
     Component[] components = toolbar.getComponents();
     for (int k=0; k<components.length; k++) 
     {
        button = (JButton)toolbar.getComponent(k);
        if (button != null)
        {
          if (button.getToolTipText().equals(toolTip))   return button;
        }
     }
     return null;
  }
     
  // Implement action listener for this menu.
  public void actionPerformed(ActionEvent ae)
  {  
     processCommand(ae);
     setEnableStatus();
  }
   
  public final void makeListener
           (JButton helpAcornButton, JMenuItem helpAcorn) throws Throwable
  {
     helpSet = Environment.getHelpSet();
     if (helpSet==null) throw new Exception();

     helpBroker = helpSet.createHelpBroker("Main_Window");
     ActionListener contentListener = new CSH.DisplayHelpFromSource(helpBroker);

     CSH.setHelpIDString(helpAcorn, "acornsoverview");
     helpAcorn.addActionListener(contentListener);

      // Attach help to the help button.
      final ActionListener newListener = new CSH.DisplayHelpFromSource(helpBroker);
      helpAcornButton.addActionListener(contentListener);
 
      helpAcornButton.addActionListener(e -> {
    		Frame frame = AppEnv.getFrame();
    		Point point = frame.getLocation();
    		helpBroker.setLocation(point);
    		ActionEvent event = (ActionEvent)(e);
    	    newListener.actionPerformed(event);
      });
 
  
  }
}
