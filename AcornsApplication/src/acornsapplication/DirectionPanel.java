/*
 * DirectionPanel.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;
import org.acorns.*;
import org.acorns.language.*;

/**
 * Class that creates a panel to allow users to navigate between lessons
 */
public class DirectionPanel extends JPanel implements ActionListener
{
   private static final long   serialVersionUID = 1;
	 
   private JButton    first, last, previous, next;

   /**
    *  Constructor to create a panel enabling users to navigate between lessons
    *  of a file
    *
    * @param active The FileObject object that occupies the data display
    * portion (right side) of the application frame
    */
   public DirectionPanel()
	  {
	     Insets insets = new Insets(5,5,5,5);
      String[] msg = LanguageText.getMessageList("acornsApplication", 59);
		
		    // Create components for this panel.
		    first = new JButton("<<");
		    first.setMargin(insets);
		    first.setBorder(BorderFactory.createRaisedBevelBorder());
		    first.setToolTipText(msg[0]);
		    first.addActionListener(this);

		    last = new JButton(">>");
		    last.setMargin(insets);
		    last.setBorder(BorderFactory.createRaisedBevelBorder());
		    last.setToolTipText(msg[1]);
		    last.addActionListener(this);

		    next = new JButton(">");
		    next.setMargin(insets);
		    next.setBorder(BorderFactory.createRaisedBevelBorder());
		    next.setToolTipText(msg[2]);
		    next.addActionListener(this);

		    previous = new JButton("<");
		    previous.setMargin(insets);
		    previous.setBorder(BorderFactory.createRaisedBevelBorder());
		    previous.setToolTipText(msg[3]);
		    previous.addActionListener(this);

      // Complete the panel.		
	 	   BoxLayout box    = new BoxLayout(this, BoxLayout.X_AXIS);
		    setLayout(box);
      add(Box.createHorizontalGlue());
		    add(first);
		    add(Box.createRigidArea(new Dimension(5,0)));
		    add(previous);
		    add(Box.createRigidArea(new Dimension(5,0)));
		    add(next);
		    add(Box.createRigidArea(new Dimension(5,0)));
		    add(last);
      add(Box.createHorizontalGlue());
   }
	
   /**
    * Listener object that responds to user navigation commands
    *
    * @param event The mouse click event object created when users issue
    * navigation commands
    */
	  public void actionPerformed(ActionEvent event)
	  {
       FileObject active = Environment.getActiveFile();
  
		     if (active==null) 
		     {
		        Toolkit.getDefaultToolkit().beep();
		        return;  // Nothing to do if no open files.
		     }
		
		     boolean ok = true;
	      JButton button = (JButton)event.getSource();
		
		     if (button==first)
		     {
		        ok = active.setActiveLesson(false, true);
		     }
		     if (button==last)
		     {
		        ok = active.setActiveLesson(true, true);
		     }
		     if (button==previous)
		     {
		        ok = active.setActiveLesson(false, false);
		     }
		     if (button==next)
		     {
		        ok = active.setActiveLesson(true, false);
		     }
		     if (!ok) Toolkit.getDefaultToolkit().beep();
	  }  // End of constructor.
   
}  // End of DirectionPanel class."Show Previous Lesson"