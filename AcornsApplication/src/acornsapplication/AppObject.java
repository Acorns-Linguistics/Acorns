/*
 * AppObject.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.*;
import javax.swing.*;
import java.nio.channels.*;

import java.io.*;
import org.w3c.dom.*;

import org.acorns.*;
import org.acorns.audio.*;
import org.acorns.lesson.*;
import org.acorns.language.*;

/**
 *   This class controls access to Acorns files.
 */
public class AppObject extends FileObject implements Serializable
{  
    private static final long serialVersionUID=1L;
    private static final String DEFAULT_LESSON_NAME = "acorns";
    
    transient String fVersion = version;  // The version of this file when read
    transient boolean converted = false;  // Version conversion flag
    transient FileLock lock;

   /** Create a new ACORNS file object
    *
    *   @param path The path to contain the folder to this file. Acorns assigns
    *   a default name (Acorn##.lnx) to this file.
    */
   public AppObject() throws IOException, NoDefaultNameException
   {  
      super();
      
      File tempFile = File.createTempFile(AppObject.DEFAULT_LESSON_NAME, ".lnx");
      tempFile.deleteOnExit();
      
      String fileName  = tempFile.getName();
      String directory = tempFile.getParent();
      openFile(directory, fileName);     

      String[] properties = getProperties();
      properties[FileObject.PATH] = AppEnv.getPath(AcornsProperties.SAVE);
      properties[FileObject.NAME] = AppObject.DEFAULT_LESSON_NAME + ".lnx";	
      setProperties(properties);
   }  // End constructor to create new file.

   /** Open an existing ACORNS file object
    *
    *   @param path The path to contain the folder of this file
    *   @param fileName The name of this file to open
    */
   public AppObject(String path, String fileName) throws IOException, InvalidFileTypeException
   {
      super();
      openFile(path, fileName);
      
      // Read header and active lesson.
      readHeader(ranOrig);
      wasSaved = true;
      dirty    = false;
      if (active!= -1) lesson = readLesson(active);
      else             lesson = null;
   }  // End constructor to open existing file.

   //---------------------------------------------------------------------
   // Perform file open operation either a new or an existing file.
   //---------------------------------------------------------------------
   private void openFile(String path, String fileName) throws IOException
   {
      properties[PATH] = path;
      properties[NAME] = fileName;
      origFile = properties[PATH] 
                  + System.getProperty("file.separator") + properties[NAME];

      File temp = File.createTempFile(origFile, null);
      temp.deleteOnExit();
      
      newFile = temp.getAbsolutePath();
      ranOrig = new RandomAccessFile(origFile, "rw");
      FileChannel channel = ranOrig.getChannel();
      lock = channel.tryLock();
      if (lock == null)
      {  
          throw new IOException(LanguageText.getMessage
                                       ("acornsApplication", 68, fileName));
      }

      ranNew  = new RandomAccessFile(newFile,  "rw");
      outFilePtr = 0;
   }  // End openFile
   
   /** Save an ACORNS file object to disk
    *
    *   @param path The path to contain the folder and name of this file
    *   @param name The name of the file object to be saved
    */
   public void saveFile(String path, String name) throws IOException
   {
	  // The following finds the most recent current version
	  for (int i=0; i<lessons; i++ )
	  {
	  	int version = panels[i].findVersion();
	  	if (version > Double.parseDouble(fVersion)) 
	  		fVersion = version + ".00";
	  }
      Lesson saveLesson = getActiveLesson();

      Lesson copyLesson;

      // Write the active lesson.
      if (saveLesson!=null)
      {   
    	  saveLesson.saveData();
          writeLesson(ranNew, saveLesson, active, false);
      }
      saveLesson = null;
         
      File tempFile = File.createTempFile("acorn", null);
      tempFile.deleteOnExit();
      String tempName = tempFile.getAbsolutePath();
      RandomAccessFile temp = new RandomAccessFile(tempName,"rw");

      // Write temp file with all of the new lessons.
      writeHeader(temp);
      outFilePtr = temp.getFilePointer();
      for (int i=0; i<lessons; i++)
      {
         copyLesson = readLesson(i);
         writeLesson(temp, copyLesson, i, true);
      }
      writeHeader(temp);  // Write with proper index pointers.
      
      // Close files.
      temp.close();
      if (lock != null && lock.isValid()) lock.release();
      ranOrig.close();
      ranNew.close();

      // Determine the old path to this file.
      String oldPath = origFile.substring(0,origFile.length()-4);
      String newPath = path + "/" + name;
      if (newPath.endsWith(".lnx"))
          newPath = newPath.substring(0,newPath.length()-4);
      
      // Delete the second level backup file.
      File oldBakF = new File(oldPath + ".bk2");
      if (oldBakF.exists()) oldBakF.delete();
      oldBakF = new File(newPath + ".bk2");
      if (oldBakF.exists()) oldBakF.delete();
      
      // Delete the original temp file.
      File tempF = new File(newFile);
      if (tempF.exists()) tempF.delete();
      
      // If the path changed, delete the backup.
      File backF = new File(oldPath + ".bak");
      File newBackF = new File(newPath + ".bak");
      if (!backF.getCanonicalPath().equals(newBackF.getCanonicalPath())) 
      { if (backF.exists()) backF.delete(); }
      backF = newBackF;
      
      // Rename files appropriately.
      File origF = new File(origFile);
      tempF      = new File(tempName);
      File newF  = new File(newFile);
      
      String  fileName = path + System.getProperty("file.separator")+name;

      if (origFile.equals(fileName))
      {  
    	 if (backF.exists())   
    	    backF.renameTo(oldBakF); // backup to old backup.
      
         origF.renameTo(backF);    // Original to the backup.
         tempF.renameTo(origF);    // temp file to the original.
      }
      else
      {  
    	 tempF.renameTo(new File(fileName));
         if (!wasSaved) origF.delete();
      }
      newF.delete();
     
      // Reopen the file.
      openFile(path, name);
      wasSaved = true;
      dirty    = false;
      AppEnv.addRecentFile(fileName);

      if (active!= -1) saveLesson = readLesson(active);
      else             saveLesson = null;
   
      displayLesson();

   }  // End save file.
   
   /** Export an ACORNS file object using XML and standard resource formats 
    *
    *   @param fileName The name of this file object
    *   @param directoryName The directory where this object is to be saved
    *   @param options The respective extension for the appropriate type of
    *   sound and image file format to export. The offsets are 
    *   Environment.SOUND_TYPE  and Environment.IMAGE_TYPE. Example values are
    *   "mp3" for sound and "jpg" for images.
    *
    *   @return null if export failed, return a string if fileName is null
    */
   public String exportFile(File fileName, File directoryName, String[] options) 
   {
	  String result = "";
	  
      try
      {
          Lesson newLesson;
    	  if (fileName!=null)
    	  {
	          for (int i=0; i<lessons; i++)
	          {
	              // Read the lesson and get its data.
	              if (i == active) 
	              {   newLesson = lesson;
	                  newLesson.saveData();
	              }
	              else  newLesson = readLesson(i);
	     
	              newLesson.export(directoryName, options, i);
                  AppEnv.setText( (100.0*i/lessons) + "%");

	         }
    	  }
    	  
          Export exportLesson = new Export(this, directoryName, options);
          if (fileName!=null)  exportLesson.outputFile(fileName);
          else result = exportLesson.outputString();
     }
     catch (Exception exception) 
     {
    	 exception.printStackTrace();
    	 return null;
     }
     return result;
  }
   
   /** Import a new ACORNS file object using XML and standard resource files
    *
    *   @param fileName The File object for the source file to be imported
    *   @param directoryName The path to contain the directory holding import 
    *   information
    *
    *   @return null if successful, error message otherwise
    */
   public String importFile(File fileName, String directoryName)
     {
      
      Import importFile;
      try
      {   // Read in the DOM and set the file header
          importFile = new Import
                  (fileName, this, directoryName);

          // Import all of the lessons in the file
          NodeList list = importFile.getLessons();
          int count = list.getLength();
          for (int i=0; i<count; i++)
          {  
        	  importFile.importLesson(list.item(i));  
          }

          // Set the default file name
          properties[NAME] = getDefaultName();

          // Display the initial lesson
          if (active==0) displayLesson();
          else           setActiveLesson(0);
      }
      catch (Exception ex) 
      {  return ex.toString();
      }
      return null;
     }
   
   /** Close this ACORNS file object
    *  @param dialog true if a close file dialog wanted.
    */
   public boolean closeFile(boolean dialog) 
   {
      try
      {
         if (dirty && dialog)
         {  String fileName = properties[PATH] 
                    + System.getProperty("file.separator") + properties[NAME];
             
            Frame frame = Environment.getRootFrame();
            File file = new File(fileName);
            int answer = JOptionPane.showConfirmDialog(frame, 
              LanguageText.getMessage("acornsApplication", 53, file.getName())
                 , LanguageText.getMessage("acornsApplication", 52)
                 , JOptionPane.YES_NO_CANCEL_OPTION);
              
            if (answer == JOptionPane.CLOSED_OPTION) return false;
            if (answer == JOptionPane.CANCEL_OPTION) return false;
             if (answer == JOptionPane.YES_OPTION) 
               saveFile(properties[PATH],properties[NAME]);             
         }

         if (lock != null && lock.isValid())  lock.release();
         ranOrig.close();  // Close the original and temp files.
         ranNew.close();
         
         File file = new File(newFile);
         file.delete();    // Delete the temp file.
         
         if (!wasSaved)    // If file was never saved, delete it.
         {
            file = new File(origFile);
            file.delete();
         }  
      }
      catch (Exception ex) { return false; }
      return true;
   }  // End of closeFile
   
   //---------------------------------------------------------------------
   // Method to read file header
   //---------------------------------------------------------------------
   private void readHeader(RandomAccessFile in) throws IOException, InvalidFileTypeException
   {
 
      // Read the primitive fields. 
      byte bytes[] = new byte[in.readInt()-12];
      lessons = in.readInt();
      active  = in.readInt();
      layer   = in.readInt();

      // Deserialize the file header.
      in.read(bytes);
      ByteArrayInputStream bIn   = new ByteArrayInputStream(bytes);
      ObjectInputStream    objIn = new ObjectInputStream(bIn);
      
      // Read fields from file header.
      try
      {
         String headerFileType    = (String)objIn.readObject();
         fVersion           = (String)objIn.readObject();
         // Verify that this is an appropriate file.
         if (!headerFileType.equals(AppObject.fileType)) 
             throw new InvalidFileTypeException();
         float thisVersion = Float.parseFloat(version);
         float fileVersion = Float.parseFloat(fVersion);
         if (!(fileVersion>=2 && fileVersion<=thisVersion))
             throw new InvalidFileTypeException();

         properties[DESC]   = (String)objIn.readObject();
         properties[LANG]   = (String)objIn.readObject();
         properties[AUTHOR] = (String)objIn.readObject();
         offsets            = (long[])objIn.readObject();      
         sizes              = (int[])objIn.readObject();
         
         String[]    infoString;
         LessonPanel lessonPanel;
         panels = new LessonPanel[offsets.length];
         
         for (int i=0; i<panels.length; i++)
         {
            infoString  = (String[]) objIn.readObject();
            if (i < lessons)
            {
               lessonPanel = new LessonPanel(infoString);
               panels[i]   = lessonPanel;
            }
            else panels[i] = null;
         }
      }
      catch (InvalidFileTypeException fnf)
      { throw new InvalidFileTypeException(); }
      
      catch (ClassNotFoundException cnf)
      {
         cnf.printStackTrace();
         try{in.close(); }catch(Exception ex) {}
         System.exit(1);
      }
      catch (Exception e)    
      { try{in.close(); }catch(Exception ex) {}  }
            
          return;
  
   }  // End of readHeader
   
   //---------------------------------------------------------------------
   // Method to write file header
   //---------------------------------------------------------------------
   private void writeHeader(RandomAccessFile out) throws IOException
   {
      // Serialize the file header.
      ByteArrayOutputStream bOut   = new ByteArrayOutputStream();
      ObjectOutputStream    objOut = new ObjectOutputStream(bOut);
      
      objOut.writeObject(fileType);
      if (converted)  objOut.writeObject(version);
      else            objOut.writeObject(fVersion);

      objOut.writeObject(properties[DESC]);
      objOut.writeObject(properties[LANG]);
      objOut.writeObject(properties[AUTHOR]);
      objOut.writeObject(offsets);     
      objOut.writeObject(sizes);
      
      for (int i=0; i<panels.length; i++)
      {
         String[] lessonInfo = new String[AcornsProperties.TYPES];
         if (panels[i] != null)
         {
            lessonInfo = panels[i].getPanelInfo();
         }
         objOut.writeObject(lessonInfo); 
      }

      byte[] bytes = bOut.toByteArray();
      // Write the header to the random access file.
      out.seek(0);
      out.writeInt(bytes.length+12);
      out.writeInt(lessons);
      out.writeInt(active);
          out.writeInt(layer);
      out.write(bytes);
   }  // End of writeHeader
   
   /** Get print data for this file object
    *
    * @return The dom object with all of the print data
    * @throws java.lang.Exception
    */
     public Document print() throws Exception
     {    Export printLesson = new Export(this, null, null);
          return printLesson.getDocument();
     }  // End of print method
   
   /**
    * Determine if this file object was ever saved
    * 
    * @return true if file was never saved
    */
   public boolean isDefaultName()   {  return !wasSaved; }
   
   /** Alter default name if file never saved
    * 
    * @param path
    * @return
    */
   public String getDefaultName()
   {
       String fileName = properties[FileObject.NAME];
       if (!wasSaved)
       {
           LessonPanel panel = panels[0];
           if (panel != null)
           {
               String newName = panel.getPanelInfo()[AcornsProperties.TITLE];
               if (newName.trim().length()!=0) fileName = newName + ".lnx";
           }
         }
         return fileName; 
   }
      

   /** Display and return the list of lessons in this file object in the lesson list
    *  panel to the left of the application frame
    */
   public LessonDisplayPanel displayLessons()
   {
      LessonDisplayPanel jpanel = null;
      
      if (!Files.getMode())
      {
         JScrollPane  lessonScroll = AppEnv.getLessonScroll();
      
         jpanel = new LessonDisplayPanel(this);

         for (int i=0; i<lessons; i++)
         {
             jpanel.add(i+1, panels[i], (i==active));
         }
         lessonScroll.setViewportView(jpanel);
      }  
      
      // Draw data information in right window.
      displayLesson();
      if (jpanel!=null) 
    	  jpanel.forceActiveVisible(active);  // Force active lesson to be visible on left
      return jpanel;
   }
   
   /** Display the active lesson in the lesson data panel to the right of the
    *  application frame
    *
    */
   public void displayLesson()
   {
      // Draw data information in right window.
      JSplitPane split = AppEnv.getSplitPane();
      int location = split.getDividerLocation();
      PlayBack.stopPlayBack();
      JFrame frame;
      if (lesson!=null)
      {  
         if (Files.getMode())
         {
             Score.reset();
             JPanel lessonPlay = lesson.play();
             Container playFrame = AppEnv.getPlayFrame();
             
             frame = (JFrame)playFrame;
             Container container = frame.getContentPane();
             
             container.removeAll();
             if (lessonPlay!=null) container.add(lessonPlay);
             container.invalidate();
             container.validate();
             container.repaint();
         }
         else  
         {
            // Get and size panel to hold the controls and the data.
            JPanel lessonData = AppEnv.getDataPanel();
            lessonData.removeAll();
            Dimension size = AppEnv.getDisplaySize();
 
            // Get the scroll pane to wrap the data panel.
            JScrollPane lessonDisplayArea = AppEnv.getDataScrollPane();
            JPanel dataPanel = null;
            
            JPanel[] displayPanels = new JPanel[2];
            displayPanels = lesson.getLessonData();
            dataPanel = displayPanels[AcornsProperties.DATA];
      
            if (dataPanel != null)
            {
               // Get control panel for this lesson type.
               JPanel lessonControls = displayPanels[AcornsProperties.CONTROLS];
               lessonControls.setMinimumSize(new Dimension(size.width,50));
               GridBagConstraints c = new GridBagConstraints();
               c.anchor = GridBagConstraints.NORTH;
               c.gridx = c.gridy = 0;
               c.weightx = c.weighty = 0;
               lessonData.add(lessonControls, c);

               c = new GridBagConstraints();
               c.anchor = GridBagConstraints.NORTH;
               c.gridx = 0;
               c.gridy = 1;
               c.weightx = c.weighty = 0.5;
               lessonDisplayArea.setViewportView(dataPanel);
               lessonData.add(lessonDisplayArea, c);
                
               // Set the size of the scrollpane with room for the scrollBar.
               final int SCROLL_WIDTH = 10;
               Dimension newSize = new Dimension
                       (size.width+SCROLL_WIDTH, size.height+SCROLL_WIDTH);
               Dimension panelSize = dataPanel.getSize();
               if (panelSize.height>size.height) 
                   newSize.width = size.width - SCROLL_WIDTH;
               if (panelSize.width>newSize.width) 
                   newSize.height = size.height - SCROLL_WIDTH;
 
               lessonData.setPreferredSize(newSize);
               lessonData.setSize(newSize);
               lessonData.setMaximumSize(newSize);
               lessonDisplayArea.setPreferredSize(newSize);
               lessonDisplayArea.setSize(newSize);
               lessonDisplayArea.setMaximumSize(newSize);  
               lessonDisplayArea.setMinimumSize(newSize);
            }
            else lessonData.add(new JPanel());      
          
            lessonData.invalidate();
            lessonData.validate();
            lessonData.scrollRectToVisible(lessonData.getBounds());
            frame = AppEnv.getFrame();
            frame.validate();
            frame.repaint();
         }
      }
      else
      {
         JPanel dataPanel = AppEnv.getDataPanel();
         dataPanel.removeAll();
         frame = AppEnv.getFrame();
         dataPanel.scrollRectToVisible(dataPanel.getBounds());
         frame.validate();
         frame.repaint();
      }
      split.setDividerLocation(location);
   }
      
   /**
    * Instantiate a new lesson
    *
    * @param info An array of strings holding lesson header information. Refer 
    * to the LessonPanel class for more information
    * @param newLessonObject Object to create or null
    */
   public boolean newLesson(String[] info, Lesson newLessonObject)
                          throws IOException, InvalidFileTypeException
   {  
      if (lessons>=MAX_LESSONS) return false;
      
      LessonPanel lessonPanel = new LessonPanel(info);
      // Save current lesson into new file.
      if (active != -1)
      {  lesson.saveData();  // Save any data not yet saved.
         if (lesson.isDirty()) writeLesson(ranNew, lesson, active, false);
      }
      
      // Create and insert new lesson into table of lessons and display.
      if (newLessonObject==null) lesson = lessonPanel.getLessonObject();
      else lesson = newLessonObject;
      
      try
      {
    	  int lessonVersion = (int)Float.parseFloat(fVersion);
    	  String packageName = lesson.getClass().getPackage().toString();
  		  try {
    		  lessonVersion = Integer.parseInt (packageName.replaceFirst("^.*\\D",""));
		  } catch (Exception e) {}

    	  if (lessonVersion > Float.parseFloat(fVersion)) fVersion = lessonVersion + ".00";
      }
      catch (Exception e) { converted = true; }
      active++;
      
      Lesson insLesson = null;
      // Make space for new lesson.
      insertElementAt(active, 0, 0, lessonPanel, insLesson); 
      
      // Save new lesson to temp file.
      dirty = true;
      writeLesson(ranNew, lesson, active, false); // Write an initial copy.
      displayLessons();
      return true;
   }  
   
   /**
    * Modify lesson header information based on a user dialog
    */
   public boolean modifyLesson() throws IOException
   {
       if (active==-1) return false;
       boolean result = panels[active].modifyPanel();
       if (!result) return false;

       // Write updated lesson to the temp file.
       writeLesson(ranNew, lesson, active, false);
       dirty = true;

       displayLessons();
       return true;
   }
   
   /** Remove the active lesson from this file object.
    */
   public void removeLesson() throws IOException
   {
      if (active==-1) return;
    
      removeElementAt(active);
      
      // Determine which lesson will now be active.
      int newActive = active;
      if (active==lessons) newActive--;
      
      active = -1; // This forces the lesson to be read.      
      if (newActive != -1) lesson = readLesson(newActive);  
      else                 lesson = null;
      
      // Redisplay the lesson list and indicate that the file needs saving.
      active = newActive;  // Set which lesson is now active.
      displayLessons();
      dirty = true;
   }

   /** Alter a lessons position in the current file object.
    */
   public void moveLesson(int number, int count) throws IOException
   { 
      if (count==0) return;
      long        lessonSpot = offsets[number];
      int         lessonSize = sizes[number];
      LessonPanel info       = panels[number];
      int newSpot            = number + count;
    
      // Write the active lesson.
      if (number>=0)
      {   
    	  if (active>=0) 
    		  lesson.saveData(); 
          writeLesson(ranNew, getActiveLesson(), number, false);
      }

      // Remove element and reinsert it appropriately.
      Lesson insLesson = null;
      removeElementAt(number);
      insertElementAt(newSpot, lessonSpot, lessonSize, info, insLesson);
      
      if (active == number) active = newSpot;
      else
      {
         if (active >= newSpot && active < number) active++;
         if (active <= newSpot && active > number) active--;
      }        
      // Adjust panel of lessons for display.
      displayLessons();
      dirty = true;
   }
   
   //---------------------------------------------------------------------
   // The following methods manipulate the list of lessons.
   //---------------------------------------------------------------------
   
   //---------------------------------------------------------------------
   // Method to insert an element at a given position.
   //---------------------------------------------------------------------
   private void insertElementAt
        (int lesson, long offset, int size, LessonPanel panel, Lesson insLesson)
   {
      // Insert into local tables.
      resize();
      for (int i=lessons; i>lesson; i--) 
      { 
         offsets[i] = offsets[i-1]; 
         sizes[i]   = sizes[i-1];
         panels[i]  = panels[i-1];
      }
      offsets[lesson] = offset;
      sizes[lesson]   = size;
      panels[lesson]  = panel;
      lessons++;
      
   }  // End of insert element at.

   private void removeElementAt(int lesson)
   {
      for (int i=lesson; i<lessons-1; i++) 
      { 
         offsets[i] = offsets[i+1]; 
         sizes[i]   = sizes[i+1];
         panels[i]  = panels[i+1];  
      }
      lessons--;
   }

   /**
    * Read a lesson from disk to memory. This method does a memory transfer
    * if it is already in memory.
    *
    * @param lessonNo The lesson number in this file
    */
   public final Lesson readLesson(int lessonNo) throws IOException
   {
      if (lessonNo>=lessons) return null;

      RandomAccessFile in;
      Lesson lessonIn = null;
      if (active==lessonNo && lesson !=null) return lesson;
      
      // Get size of lesson object and read into a byte array.
      long lessonSpot = offsets[lessonNo];
      if (lessonSpot > 0) in = ranOrig;
      else                
      {
         lessonSpot *= -1;
         in = ranNew;
      }
      int size = sizes[lessonNo];
      
      byte[] bytes = new byte[size];
      in.seek(lessonSpot);
      in.read(bytes);
      
      // Deserialize the lesson.
      try
      {
         ByteArrayInputStream bIn   = new ByteArrayInputStream(bytes);
         ObjectInputStream    objIn = new ObjectInputStream(bIn);
         float fileVersion = Float.parseFloat(fVersion);
         Object objData = objIn.readObject();
         try 
         { lessonIn = (Lesson)objData;
           Lesson newLesson = lessonIn.convert(fileVersion);
           if (newLesson!=lessonIn)
           {  lessonIn = newLesson;
              converted = true;
           }

         }
         catch (ClassCastException ex)
         {   
             Tools.Lesson oldLessonIn = (Tools.Lesson)objData;
             lessonIn = oldLessonIn.convert(fileVersion);
             if (lessonIn==null)
             {
                 AppEnv.setText(LanguageText.getMessage
                        ("acornsApplication", 54, "" + fileVersion, version));
                     throw new IOException(LanguageText.getMessage
                             ("acornsApplication", 55, version));
             }
             converted = true;
         }
         lessonIn.setDirty(false);
         lessonIn.initializeLesson();
      }
      catch (ClassNotFoundException cnf)
      {
         AppEnv.setText(LanguageText.getMessage("acornsApplication", 56));
      }           
      return lessonIn;
      
   }  // End of readLesson
   
}  // End of AppObject class
