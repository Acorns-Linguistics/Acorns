/*
 * FileObject.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package org.acorns;

import java.awt.*;
import java.io.*;
import javax.swing.*;

import org.acorns.lesson.*;
import org.acorns.language.*;

/**
 *   This class controls access to Acorns files.
 */
public abstract class FileObject implements Serializable
{  
   protected static final int     INITIAL_INDEX_SIZE = 8;
   private   static final String  VERSION = "10.0";
   private   static final String  PLUGIN_VERSION = "V9";
      
   // lnx file format indicator.
   protected static final String fileType  = "Acorns";
   protected static final String version   = VERSION;
   protected static final String PREFIX    = "acorn";
   protected static final String EXTENSION = "lnx";
   
   private static final long serialVersionUID=1L;

   // File properties  
   
   /** Path to the folder containing this file */
   public static final int PATH=0;
   /** Name of this file */
   public static final int NAME=1;
   /** File property containing description of this file */
   public static final int DESC=2;
   /** File property containing the languages used in this file */
   public static final int LANG=3;
   /** File property containing the author of this file */
   public static final  int AUTHOR=4;
   private static final int PARAMS=5;
   
   /** Maximum number of lessons in a file */
   protected final static int MAX_LESSONS = 999;   
   protected String[] properties  = new String[PARAMS]; // File properties.
   
   // File status flags.
   protected transient boolean  wasSaved = false;  // File was ever saved.
   protected transient boolean  dirty    = false;  // File has changed.
   
   // File handles.
   protected transient String origFile;            // Original file name.
   protected transient RandomAccessFile ranOrig;   // Handle to the original file.
   protected transient String newFile;             // Temp file name.
   protected transient RandomAccessFile ranNew;    // Handle to the temp file.
   protected transient long outFilePtr;            // Pointer to the output file.
   
   // Information pertaining to each lesson.
   protected int            lessons;     // Number of lessons.
   protected long[]         offsets;     // File offsets to start of each lesson.
   protected int[]          sizes;       // Byte sizes of each lesson.
   protected LessonPanel[]  panels;      // Header information for each lesson.
   
   // Global information.
   protected int           active;      // Active lesson number.
   protected int           layer;       // layer value (1->10) for this file.
   protected Lesson        lesson;      // The active lesson object.
      
   /**
    *  Method to initialize the lessons data structures.
    */
    public FileObject()
    {
      offsets    = new long[INITIAL_INDEX_SIZE];
      sizes      = new int[INITIAL_INDEX_SIZE];
      panels     = new LessonPanel[INITIAL_INDEX_SIZE];
      properties[DESC]   = "";
      properties[LANG]   = "";
      properties[AUTHOR] = "";
      
      lessons  = 0;
      active   = -1;
      lesson   = null;
      wasSaved = false;
      dirty    = true;
      layer    = 1;
      Environment.setMode(false);
    }
	 
	/** Mark this file object dirty indicating that changes have occurred. Users
    *  get a chance to save dirty files before they are closed or before the 
    *  ACORNS  program exits.
    */  
    public void fileChanged()   
    {  dirty = true;
       if (Environment.isApplet())
           Environment.getEnvironment().writeStatus();
    }
      
   /** Set properties describing this file object
    *
    * @param description Short description of this file
    * @param language The languages used in this file
    * @param author   The author who created this file
    */
   public void setProperties(String description, String language, String author) 
   { 
      properties[DESC]   = description;
      properties[LANG]   = language;
      properties[AUTHOR] = author;
      dirty = true;
   }
   
   /** Method to update file properties
    * 
    * @param properties Array of file properties
    */
   public void setProperties(String[] properties)
   {
	   this.properties = properties.clone();
   }
   
   /** Get file properties
    *
    * @return an array of strings with the indices:<break/>
    * WebObject.DESC for description<break/>
    * WebObject.LANG for language<break/>
    * WebObject.AUTHRO for author<break/>
    */
    public String[] getProperties()  
    { 
      String[] returnProperties = properties.clone();
      return returnProperties;  
    }
   
  
   /**
    * Set the lesson with the designated index to be active
    *
    * @param lessonNo The index of the lesson to be made active
    *
    * @return The JPanel holding the lesson header information, or null if
    * the command fails
    */
   public JPanel setActiveLesson(int lessonNo) 
   {
      if (lessonNo==active)  return null;  // Nothing to do if there is no change.
      if (lessonNo<0)        return null;
      if (lessonNo>=lessons) return null;
      try
      {
         lesson.saveData(); // Save any data that was pending.
         lesson.getLessonHeader(); // Commit partially edited data.
         Lesson newLesson = readLesson(lessonNo);
      
         // Verify that we can switch to this lesson.      
         if (Environment.getMode() && !newLesson.isPlayable())
         {
             JPanel dialog = new JPanel();
             BoxLayout box = new BoxLayout(dialog, BoxLayout.Y_AXIS);
             dialog.setLayout(box);
               
             JLabel message 
                 = new JLabel(LanguageText.getMessage("acornsApplication", 32));
             message.setAlignmentX(Component.CENTER_ALIGNMENT);                                
             dialog.add(message);
               
             String title = Environment.getTitle();
             Frame frame = Environment.getRootFrame();
             JOptionPane.showMessageDialog
                    (frame, dialog, title, JOptionPane.PLAIN_MESSAGE); 
             return null;
         }
       
         JPanel lessonPanel = null;

         // Write current lesson and read the new one.
         if (lesson.isDirty()) writeLesson(ranNew, lesson, active, false);
            
         lesson = newLesson;
         active = lessonNo;   
      
         // Redisplay the lesson list and indicate that the file needs saving.
         lessonPanel = displayLessons();
       
         dirty = true;
         return lessonPanel;
      }
      catch (IOException ex) {Environment.setText(ex.toString());}
      return null;
   }

    /**
    * Get the active lesson object
    */
   public Lesson getActiveLesson() {return lesson;}
	
	 /**
    *
    * Get panel holding lesson header information
    */
    public LessonPanel getLessonInfo() 
    {
      if (active==-1) return null;
      else return panels[active];
    }

    /** Get header information of a non active lesson */
    public String[] getLessonHeader(int num)
    {   return panels[num].getPanelInfo(); }
 
   /**
    * Traverse between lessons
    *
    * @param forward true if traversing forward, false if traversing back
    * @param endLesson true if traversing to either first or last lesson. false
    * if traversing to either next or previous lesson
    *
    * @return true if traverse is successful
    */
   public boolean setActiveLesson(boolean forward, boolean endLesson)
   {  
      boolean result = true;
      if (active==-1) return false;
      
      int newActive;
      if (forward)
      {  if (endLesson) newActive = lessons-1;
         else           newActive = active+1;
      }
      else
      {  if (endLesson) newActive = 0;
         else           newActive = active-1;
      }
      if (newActive>=lessons) 
      {
         newActive = lessons-1;
         result    = false;
      }
      if (newActive<0)        
      {
         newActive = 0;
         result    = false;
      }
      setActiveLesson(newActive);
      return result;
   }
   
   /**
    * Find the lesson with the matching name active
    *
    * @param name Name of the lesson to be made active
    *
    * @return The JPanel holding the lesson header information, or null if
    * the command fails
    */
   public boolean setActiveLesson(String name)
   {  String[] info;
      String infoName, srchName = name.toLowerCase().trim();
		    
      if (name!=null) 
      {  for (int i=0; i<lessons; i++)
         {
            info = panels[i].getPanelInfo();
	    infoName = info[AcornsProperties.NAME].toLowerCase().trim();
            if (srchName.equals(infoName))
            {  setActiveLesson(i);
               return true;
            }
         }
      }
      JOptionPane.showMessageDialog
             (null, LanguageText.getMessage("acornsApplication", 33)
              , Environment.getTitle(), JOptionPane.PLAIN_MESSAGE); 
      return false;
   }
   
   //---------------------------------------------------------------------
   // Method to expand the index if needed.     
   //---------------------------------------------------------------------
   protected void resize()
   {
      if (lessons == offsets.length)
      {
         long[] newOffsets       = new long[2*offsets.length];
         int[] newSizes          = new int[2*offsets.length];
         LessonPanel[] newPanels = new LessonPanel[2*offsets.length];
         
         for (int i=0; i<offsets.length; i++)  
         {
            newOffsets[i] = offsets[i];
            newSizes[i]   = sizes[i];
            newPanels[i]  = panels[i];
         }
         offsets = newOffsets;
         sizes   = newSizes;
         panels  = newPanels;
      }
   }
	
	/*
    * Write a lesson in a file the the destination file
    *
    * @param out Destination Random output stream
    * @param lessonOut The Lesson object
    * @param number The lesson number in this file
    * @param orig true if program is to write this lesson to a temporary file
    */
   protected void writeLesson(RandomAccessFile out, Lesson lessonOut
                               , int number, boolean orig) throws IOException
   {
      // Serialize the lesson.
      ByteArrayOutputStream bOut   = new ByteArrayOutputStream();
      ObjectOutputStream    objOut = new ObjectOutputStream(bOut);
      objOut.writeObject(lessonOut);
      byte[] bytes = bOut.toByteArray();

      // Write the lesson to the random access file.
      out.seek(outFilePtr);
      long spot = outFilePtr;
      out.write(bytes);
      long newSpot  = out.getFilePointer();
      outFilePtr    = newSpot;
      System.out.println(bytes.length + " " +  (newSpot - spot) + " " + sizes[number] + " " + orig);
      sizes[number] = (int)(newSpot - spot);
      if (!orig) spot *= -1;
      offsets[number] = spot;
		
      lessonOut.setDirty(false);
   }  // End of writeLesson   

   public static String getVersion() { return VERSION;  }
   public static String getPluginVersion() { return PLUGIN_VERSION; }
   
   // Abstract methods.
   public abstract Lesson readLesson(int lessonNo) throws IOException;
   public abstract void moveLesson(int number, int count) throws IOException;
   public abstract boolean newLesson(String[] info, Lesson newLesson) 
                                      throws InvalidFileTypeException, IOException;
   public abstract JPanel displayLessons();
	  public abstract void displayLesson();	 
}  // End of FileObject class
