/*
 * Properties.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */

package org.acorns;

import java.beans.*;
import javax.swing.*;
import java.awt.*;
import java.net.*;
import org.acorns.data.*;
import org.acorns.lesson.*;
import javax.help.*;

public class Properties extends AcornsProperties
{
   public Properties()
   {   // Register this object as a property change listener.
       String listener = "Acorns";
        PropertyChangeListener[] pcl 
            = Toolkit.getDefaultToolkit().getPropertyChangeListeners(listener);
        for (int i=0; i<pcl.length; i++)
        {   Toolkit.getDefaultToolkit().removePropertyChangeListener
                                                        (listener, pcl[i]); }
        Toolkit.getDefaultToolkit().addPropertyChangeListener(listener, this);
   }
   
   /** Get the current Acorns property listener
    * 
    * @return AcornsProperties object, or null if not found.
    */
   public static AcornsProperties getAcornsProperties()
   {
          PropertyChangeListener[] pcl 
                  = Toolkit.getDefaultToolkit()
                                 .getPropertyChangeListeners("Acorns");
          for (int i=0; i<pcl.length; i++)
          {
              try
              {
                  return (AcornsProperties)pcl[i];
              }
              catch (ClassCastException ex) {}
          }
          return null;
   }      // End of getAcornsProperties
    
  /**  Indicate that this lesson has changed.
    */
   public boolean setFileDirty()
   {   
	   if (Environment.getActiveFile() == null) 
       {   return false;
       }
       FileObject file = Environment.getActiveFile();
       file.fileChanged();
       Lesson lesson = file.getActiveLesson();
       if (lesson!=null) lesson.setDirty(true);
       return true;
   }
   
   /**  Get an icon ImageIcon object 
    *   @param iconName Name of the icon name.
    *   @param size     Size to scale the icon.
    *   @return icon ImageIcon object or null.
    */
  public ImageIcon getIcon(int iconName, int size)
  {   return Environment.getIcon(iconName, size);  }
  
  public SoundData getSound(int soundName)
  {  return Environment.getSound(soundName);  }
   
   /** Get size of the display panel.
    *  @return Size of the area where the lesson will display.
    */
   public Dimension getDisplaySize() {return Environment.getDisplaySize();}
   
   /** Get size of the main applicaton frame.
    *  @return Size of the application frame.
    */
   public Dimension getFrameSize() 
   {  return new Dimension(Environment.getPlayFrame().getSize()); }

   /** Get size of the size of the applet or entire screen.
    *  @return Size of the application frame.
    */
   public Dimension getScreenSize() 
   {  return new Dimension(Environment.getScreenSize()); }
      
   /** Get help set URL
    *  @return help set url or null.
    */
   public HelpSet getHelpSet()  {  return Environment.getHelpSet();  }
   
   /** Enable or disable menu buttons appropriately.
    */
   public void activateMenuItems() 
   {  
	   Environment.setEnable();  
   }
   
  /** Get Header Information for this lesson.
    *  @return string array containing header information.
    */
   public String[] getLessonHeader()
   {
      LessonPanel lessonHeader = Environment.getActiveFile().getLessonInfo();
      if (lessonHeader==null) return null;
      return lessonHeader.getPanelInfo();
   }
   
   /** Get the path to types of files that the user browses to.
    *  @param option type of file
    */
   public String getPath(int option)   
   {  return Environment.getPath(option); }
      
   /** Set the path to a file accessed by the user.
    *  @param option type of file
    *  @param path path to the folder containing the filefile
    */
   public void setPath(int option, String path)
   {  Environment.setPath(option, path);  }
   
   /** Determine if this is an applet executing.
    *  @return true if applet, false otherwise
    */
   public boolean isApplet() { return Environment.isApplet(); }
   
   /** Determine if we are in play mode.
    *  @return true if yes.
    */
   public boolean isPlay()  { return Environment.getMode(); }
   
   /** Get root frame (playFrame or application frame)
    * 
    */
   public Frame getRootFrame() { return Environment.getRootFrame(); }
   
   /** Get speech option array.
    *  @return array of speech options.
    */
   public boolean[] getOptions() {  return Environment.getOptions();  }
   
   /** Update speech option array.
    *  @param options boolean array of speech options
    */
   public void setOptions(boolean[] options)
   {  Environment.setOptions(options);   }
   
   /** Set active lesson based on anchor tag text.
    *  @param hyperlink text to another lesson
    */
   public boolean setActiveLesson(String hyperlink)
   {  return Environment.getActiveFile().setActiveLesson(hyperlink); }
   
   /** Set active lesson based on navigation buttons
    *  @param direction false to go towards front, true to go towards back
    *  @param distance  false for adjacent lesson, true to go to end
    */
   public boolean setActiveLesson(boolean direction, boolean distance)
   { return Environment.getActiveFile().setActiveLesson(direction, distance);  }
   
   /** Redisplay the current lesson.
    */
   public void displayLesson() 
   {   System.gc();
       Environment.getActiveFile().displayLesson(); }
   
   /** Get application title for display in dialog frames
    *  @return application title
    */
   public String getTitle()  {  return Environment.getTitle(); }
   
	/** Output an error message 
	 *  @param msg to output
	 */
	 public void setText(String msg) 
	 {  Environment.setText(msg);
	 }
	 
   /** Exit play mode and return to setup mode. */
   public void exitPlayMode()  { Environment.exitPlayMode(); }
   
   /** Empty Property change listener method.
    */
   public void propertyChange(PropertyChangeEvent pce)  {}

   /** Method to load a picture using a chooser dialog
    *  @return URL object of null if operation failed
    */
   public URL getPicture()
   {
       URL url = null;
       try { url = Environment.getEnvironment().getPicture(); }
       catch (Exception e) {}
       return url;
   }
}

