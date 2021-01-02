/*
 * Files.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
 package acornsapplication;

import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;

import org.acorns.audio.SoundDefaults;
import org.acorns.lesson.*;

/**
 *  <p>The Status class contains general methods needed to control the
 *  windows environment for the Acorn project.</p>
 */
public class Files
{
   /** Maximum number of recently opened files remembered by the Acorn program */
   public static final int MAX_FILES   = 8;   // Maximum # of recent files.

   private final static String separator = System.getProperty("file.separator");

   private static JMenuItem[]  windowItems;  // Open file menu selections.
   private static ArrayList<String> tabNames;   // Tabs for the tabbed pane.
   private static JTabbedPane  tabbedPane;   // Tabbed pane for data windows.

   private static boolean      tabs;         // Tab or full view.
   
   private static ArrayList<AppObject> files;  // Array of open files.
   
   /**  Store the array of possible selections from the window menu
    *
    *  @param windows Array of JMenuItem selections
    */
   public static void putWindowItems(JMenuItem[] windows)   
   {  windowItems = windows;   }
   
   /**
    * Method to close all open files and write system status data to disk
    */
   public static boolean closeAllFiles() 
   {  
       AppObject oldActive;
   
       // Close all open files. 
       while ( (oldActive = getActiveFile()) != null) 
       { 
          if (!oldActive.closeFile(true)) return false; 
            else  removeOpenFile(oldActive);
       }
       return true; 
   }
      
   /**
    * Accessor method to return whether system is in set-up or execute mode
    * @return true if in play mode, false otherwise
    */
   public static boolean getMode() {return AppEnv.getMode();}
   /**
    * Mutator method to set system into either set-up or execute mode
    */
   public static void setMode(boolean playMode)    
   {  AppEnv.setMode(playMode); }
   

    /** Determine if the maximum number of files are open
     */
   public static boolean isMaxFilesOpen()
   {
       return files.size()>=MAX_FILES;
   }
   
   /** Determine if a particular file is open
    *
    * @param newFile Name of file to determine if it is open
    *
    * @return true if the file is open
    */
   public static boolean isFileOpen(String newFile)
   {
       return (findOpenFile(newFile) != null);
   }
   
   /** Find the File Object associated with the open file
    *
    * @param fileName Name of the file
    *
    * @return null if file is not open
    */
   public static AppObject findOpenFile(String fileName)
   {
        int index = -1;
        index = tabNames.indexOf(fileName);
        if (index<0) return null;
      
        AppObject file = files.get(index);
          return file;
     }
      
   /** Add object to the list of open files
    *
    * @param file File Object to add
    */
   public static void addOpenFile(AppObject file)
   { 
      files.add(file);
      AppEnv.setActiveFile(file);
      
      // Add a pane and a tab name to the list of open files.
      String[] properties = file.getProperties();
      String   fileName   
           = properties[AppObject.PATH]+separator+properties[AppObject.NAME];

      // If application is sand boxed, remove the app container from the path
      String path = SoundDefaults.normalizeFilePath(fileName);
      tabNames.add(path);
      
      // If this is full view, we need to change the panel shown.
      if (tabs)  {    setTabbedPanes(files.size()-1);  }

      // Display this file.   
      JPanel lessons = AppEnv.getLessonScrollPanel();
      lessons.setVisible(true);
      
      // If this is the first file, the lesson list and current lesson
      //    will be displayed by the split pane listener.
      if (files.size() == 1) 
           AppEnv.getSplitPane().setDividerLocation(AppEnv.LESSON_WIDTH);
      else 
    	  getActiveFile().displayLessons();

      updateWindowMenu();
   }

   /** Remove file object from the open file list
    *
    * @param oldActive AppObject to remove
    */
     public static AppObject removeOpenFile(AppObject oldActive)
     {
        int index = files.indexOf(oldActive);
        files.remove(index); 
        tabNames.remove(index);
      
          if (files.isEmpty()) 
          {
             JPanel lessons = AppEnv.getLessonScrollPanel();
             lessons.setVisible(false);

             AppEnv.setActiveFile(null);
             JPanel lessonData = AppEnv.getDataPanel();
             lessonData.removeAll();
             tabs = false;
             lessonData.add(new JPanel());
             AppEnv.getSplitPane().setRightComponent(lessonData);
             tabbedPane.removeAll();
             tabs = false;
          }
          else
          {
             AppEnv.setActiveFile(files.get(0));
             if (tabs)  { setTabbedPanes(0); }
             getActiveFile().displayLessons();
          }
          if (getActiveFile() == null) 
          {  AppEnv.getLessonScrollPanel().setVisible(false);  }
          updateWindowMenu();
          return oldActive;
     }

   /** Switch between tab and full view of open files
    *
    * @param tabView true if we should switch to the tabbed view
    */
     public static void switchView(boolean tabView)
     {
        if (tabs == tabView) return;
      
        int index = files.indexOf(getActiveFile());
        if (tabView)  
        {  setTabbedPanes(index);
           tabs = tabView;
        }
        else
        {
            // Add panel to scroll bar.
            tabbedPane.removeAll();
            AppEnv.getSplitPane().setRightComponent(AppEnv.getDataPanel());
            tabs = tabView;  // Indicate the new view.
        }
        getActiveFile().displayLessons();
     }   
   
   /** Modify tabbed pane
    *  @param index Index to switch view to.
    */
   private static void setTabbedPanes(int index)
   {   
       makeTabbedPane();
       for (int i=0; i<files.size(); i++)
          tabbedPane.addTab(new File(tabNames.get(i)).getName(), null);
        
       if (files.isEmpty()) return;
       tabbedPane.setComponentAt(index, AppEnv.getDataPanel());
       AppEnv.getSplitPane().setRightComponent(tabbedPane);
       tabbedPane.setSelectedIndex(index);       
   }

   /**
    *  Initialize the list of open files at start up time
    */
   public static void startUp()
   {
      tabs = false;  // Start off with full view.
      AppEnv.setMode(false);  // Start in setup mode.
      makeTabbedPane();       // Create the JTabbed Pane.
      tabNames = new ArrayList<String>();
      files    = new ArrayList<AppObject>();
   }
   
   /** Create a new tabbed pane with a listener */
   private static void makeTabbedPane()
   {
      tabbedPane = new JTabbedPane();
      tabbedPane.addChangeListener(
         new ChangeListener()
         {
            public void stateChanged(ChangeEvent event)
            {
               int index = tabbedPane.getSelectedIndex();
               if (index>=0)
               {
                    AppEnv.setActiveFile(files.get(index));
                    getActiveFile().displayLessons();
                    updateWindowMenu();                                       
               }
            }
         }  
      );
   }
   
   /** Update the list of open items on the window's menu
    */
     public static void updateWindowMenu()
     {
        AppObject fileObject;
        String[]   properties; 
        ImageIcon image = null;
        String    fileName, path;
      
        for (int i=0; i<files.size(); i++)
        {
           fileObject = files.get(i);
           properties = fileObject.getProperties();
           fileName   
             = properties[AppObject.PATH]+separator+properties[AppObject.NAME];
           path = SoundDefaults.normalizeFilePath(fileName);
           tabNames.set(i, path);
           if (tabs && tabbedPane.getTabCount()>i) 
               tabbedPane.setTitleAt(i, new File(fileName).getName());

           String name = "" + (i+1) + " " + path;
           windowItems[i].setText(name);
           windowItems[i].setMnemonic(Character.forDigit(i+1,10));
         
           if (getActiveFile() == fileObject)
                image  = AppEnv.getIcon
                        (AcornsProperties.TICK, -1);
           else image  = AppEnv.getIcon
                         (AcornsProperties.BLANK, -1);
         
           windowItems[i].setIcon(image);
           windowItems[i].setVisible(true);
        }
        for (int i=files.size(); i<MAX_FILES; i++)
        {  windowItems[i].setVisible(false);   }

     }
   
   /** Make to another open file active
    */ 
    public static boolean switchWindow(String newFile)
     {
          int index = tabNames.indexOf(newFile);
          if (index<0) return false;
      
          if (tabs)    {  setTabbedPanes(index); }
          AppEnv.setActiveFile(files.get(index));
          getActiveFile().displayLessons();
          
          updateWindowMenu();
          return true;
     }
	  
	/** Get the active file casted appropriately.
	 *  Note: this method is called by application methods.
	 */
	public static AppObject getActiveFile()
	{  return (AppObject)AppEnv.getActiveFile();
	}
}