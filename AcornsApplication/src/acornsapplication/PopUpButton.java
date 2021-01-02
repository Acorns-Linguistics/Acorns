/*
 * PopUpButton.java
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

/** Button if pressed allows lessons to be modified or removed */
public class PopUpButton extends JButton 
{
   private final static long serialVersionUID = 1;
	
   private JPopupMenu popup;
   private JMenuItem  modify, delete, help;
    
   /** Create the button with the action listener for lesson commands */
   public PopUpButton(String title)
   {  
       super(title);
       String[] msgs = LanguageText.getMessageList("acornsApplication",140);
       popup = new JPopupMenu();
       modify = new JMenuItem(msgs[0]);
       modify.addActionListener(popupActionListener);
       popup.add(modify);
				
       delete = new JMenuItem(msgs[1]);
       delete.addActionListener(popupActionListener);
       popup.add(delete);
		
       help = getHelpItem(msgs);
       if (help != null) popup.add(help);
		
       setFont(new Font("MonoSpaced", Font.BOLD, 14) );
       setMargin(new Insets(0,0,0,0));
       setBorder(BorderFactory.createRaisedBevelBorder());
       setToolTipText(msgs[2]);
       setComponentPopupMenu(popup);
       addMouseListener(mouseListener);

       setPreferredSize(new Dimension(100, 25));
       setMaximumSize(getPreferredSize());
       setSize(getPreferredSize());
   }
	
	   
   //------------------------------------------------------------
   // Method to create a menu item attached to the help system.
   //------------------------------------------------------------
   private JMenuItem getHelpItem(String[] msgs)
   {
 
      // Create help triggered from the help button.  
      try
      {
         JMenuItem helpItem = new JMenuItem(msgs[3]);
         helpItem.setBorder(BorderFactory.createEtchedBorder());
         helpItem.setToolTipText(msgs[4]);
  
         HelpSet helpSet = Environment.getHelpSet();
         if (helpSet==null) throw new Exception();
   
         CSH.setHelpIDString(helpItem, "LessonListLesson");
         helpItem.addActionListener(
               new CSH.DisplayHelpFromFocus(helpSet
                           , "javax.help.SecondaryWindow", null));
         return helpItem;
      }
      catch (Throwable t) 
      { }
      return null;
   }

   // Create the action listener.
   private ActionListener popupActionListener = new ActionListener()
   {
      public void actionPerformed(ActionEvent event)
      {
          AppObject active = Files.getActiveFile();
          String[] feedback
                  = LanguageText.getMessageList("acornsApplication", 141);
          try
          {   if (event.getSource()==delete)
              {
                 active.removeLesson();
                 Environment.setText(feedback[0]);
              }
              if (event.getSource()==modify)
              {
                  if (active.modifyLesson())
                        Environment.setText(feedback[1]);
                  else
                  {   Environment.setText
                        (LanguageText.getMessage("acornsApplication", 45));
                  }
              }
          }
          catch (Exception e)
          {   Environment.setText
                 (LanguageText.getMessage("acornsApplication", 64));
          }

            AcornMenu.setEnable();
      }
   };
	
   // Create the mouse listener.
   MouseListener mouseListener = new MouseAdapter()
   {
      public @Override void mousePressed(MouseEvent event)
      {
           boolean flag = false;

           AppObject active = Files.getActiveFile();
           if (active !=null) flag = (active.getActiveLesson()!= null);

           modify.setEnabled(flag);
	   delete.setEnabled(flag);

	   popup.show(event.getComponent(), event.getX(), event.getY());
       }
   };
}
