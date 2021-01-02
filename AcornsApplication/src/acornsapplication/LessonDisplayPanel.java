/*
 * LessonsDisplayPanel.java
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
import javax.swing.border.*;
import java.io.*;
import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.language.*;

/** Panel appearing to the left of the application frame and displaying all
 *  lessons in a file
 */
public class LessonDisplayPanel extends JPanel 
            implements MouseListener, MouseMotionListener
{
   private final static long serialVersionUID = 1;
	
   private final static Dimension SPACER    = new Dimension(0,10);
   private final static Color     COLOR     = new Color(150,150,150,150);
   private final static int       ICON_SIZE = 20;								
   private final static Border matte
	                     = BorderFactory.createMatteBorder(3,3,3,3, Color.RED);
								
   private FileObject  file;								
   private int         startPosition, endPosition, thisPosition;
   private Point       mouseAt;
   private boolean     drag;
   private ImageIcon   icon;
	
   /** Constructor to display file header information for all lessons in an
    *  open file
    *
    * @param file An Open File object
    */
   public LessonDisplayPanel(FileObject file)
   {
        this.file   = file;
        drag        = false;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        addMouseListener(this);
        addMouseMotionListener(this);

        // Create drag icon.
        icon = Environment.getIcon(AcornsProperties.ACORN, ICON_SIZE);
   }
		
   /** Method to force the active lesson to be visible in the panel
     */
   public void forceActiveVisible(int lessonNo)
   {
      int activeComponent = 2*lessonNo + 1;
      if (getComponentCount()<=activeComponent) return;
      if (activeComponent<0) return;
      JComponent panel = (JComponent)getComponent(activeComponent);

      // Need to scroll to visible before the bounds method works.
      Rectangle bounds = panel.getBounds();
      scrollRectToVisible(bounds);
      repaint();
   }
	
   /** Add header information for a lesson to the panel
     *
     * @param number lesson number
     * @param panel  panel holding the lesson header information
     * @param active true if number is the active lesson
     */
    public void add(int number, JPanel panel, boolean active)
    {
       add(Box.createRigidArea(SPACER));
       String lessonData = LanguageText.getMessage("acornsApplication", 138);
       Border title = BorderFactory.createTitledBorder(lessonData+" "+number);
       Border compound = BorderFactory.createCompoundBorder(title, matte);

       if (active)
       {  panel.setBorder(compound);
       }
       else  panel.setBorder(title);
       add(panel);
    }
	
    /** Repaint the pane during a drag operation
     *
     * @param page object to draw into during the repaint operation
     */
       public @Override void paintComponent(Graphics page)
       {
          super.paintComponent(page);

          Dimension size = getSize();
          if (drag)
          {
              page.setColor(COLOR);
              page.drawLine(0, mouseAt.y, size.width, mouseAt.y);
              icon.paintIcon
                   (this, page, mouseAt.x-ICON_SIZE/2, mouseAt.y-ICON_SIZE/2);
          }
          if (2*thisPosition+1<getComponentCount())
          {
             LessonPanel panel = (LessonPanel)getComponent(2*thisPosition+1);
             panel.paintIt(mouseAt, icon, ICON_SIZE/2, drag);
          }
       }

    /** Change which lesson is active in response to a mouse click
     *
     * @param event The object thrown as a result of a mouse click
     */
      public void mouseClicked(MouseEvent event)
      {
         Rectangle rect;
         JPanel    panel;

         int count = getComponentCount();
         for (int i=1; i<count; i+=2)
         {
            panel = (JPanel)getComponent(i);
            rect = panel.getBounds();
            if (rect.contains(event.getPoint()))
            {
               file.setActiveLesson(i/2);
               return;
            }
         }
       }  // End of mouse clicked.

   /* Begin dragging a lesson to a new file position when the mouse is pressed
    *
    * @param Object thrown in response to a mouse press operation
    */
   public void mousePressed(MouseEvent event)
   {
      startPosition = findPosition(event.getPoint());
   }

   /* Drop lesson at a lesson to a new file position when the mouse is released
    *
    * @param Object thrown in response to a mouse press operation
    */
   public void mouseReleased(MouseEvent event)
   {
      endPosition  = findPosition(event.getPoint());
      if (startPosition >= getComponentCount()/2) return;

      thisPosition = endPosition;
      drag = false;

      FileObject active = Environment.getActiveFile();
      try
      {
         int sizeOfMove;
         if (endPosition!=startPosition)
         {
            if (endPosition>startPosition)
                 sizeOfMove = endPosition - startPosition - 1;
            else sizeOfMove = endPosition - startPosition;
            active.moveLesson(startPosition, sizeOfMove);
         }
         else
         {
             active.setActiveLesson(startPosition);
         }
      }
      catch (IOException ex) { Environment.setText(ex.toString()); }
     
      repaint();
   }

    /** Unused mouse over event
     *
     * @param event Object thrown when the mouse is over the panel
     */
   public void mouseEntered(MouseEvent event)	{}
    /** Unused mouse not over event
     *
     *  @param event Object thrown when the mouse leaves the panel
     */
   public void mouseExited(MouseEvent event)	  	{}
	
    /* Draw line and icon during a drag lesson operation
     *
     * @param Object thrown in response to a mouse press operation
     */
    public void mouseDragged(MouseEvent event)
    {
      if (startPosition >= getComponentCount()/2) return;

      mouseAt = event.getPoint();
      thisPosition = findPosition(mouseAt);
      Rectangle rectangle = new Rectangle(mouseAt.x, mouseAt.y, 1, 50);
      drag = true;

      scrollRectToVisible(rectangle);
      repaint();
    }
	
   /** Unused event triggered when the mouse moves
    *
    * @param event Object thrown when the mouse is over the panel
    */
   public void mouseMoved(MouseEvent event)     {}
	
   //------------------------------------------------------------
   // Method to find which lesson the mouse if over.
   //------------------------------------------------------------
   private int findPosition(Point point)
   {
      Rectangle rect;
      JPanel    panel;

      int  count    = getComponentCount();
      int  position = -1;
      for (int i=1; i<count; i+=2)
      {
         panel = (JPanel)getComponent(i);
         rect = panel.getBounds();
         if (rect.contains(point))
         {
            position = i/2;
            return position;
         }
         if (rect.y + rect.height >= point.y)
         {
            position = i/2;
            return position;
         }
      }
      return count/2;
   }
}
