/*
 *
 * AppEnv.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import javax.swing.*;

import java.awt.*;

import javax.imageio.*;
import java.io.*;
import java.net.*;

import javax.swing.plaf.basic.*;
import java.awt.event.*;
import java.beans.*;
import java.awt.dnd.*;

import org.acorns.*;

import org.acorns.lesson.*;
import org.acorns.editor.*;
import org.acorns.data.*;
import org.acorns.audio.*;
import org.acorns.language.*;
import org.acorns.lib.DialogFilter;
import org.acorns.video.VideoDropTarget;

/**
 *  <p>The Environment class contains general methods needed to control the
 *  windows environment for the Acorns project.</p>
 */
public class AppEnv extends Environment
{ 
    /** Width of primary application frame */
    private static final int   WIDTH=900;
    /** Height of primary application frame */
    private static final int   HEIGHT=600;
    /** Width of panel of lessons in pixels */
    public static final int   LESSON_WIDTH=230;
    /** Height of the lesson control bar in pixels */
    public static final int  CONTROL_HEIGHT=30;
    /** Height of the frame border on the top of the display. */
    private static final int FRAME_BORDER=40;
    /** Scroll bar increment */
    private static final int SCROLL_INCREMENT=50;
    
    /** No drop in file chooser */
    public final static int NONE = 0;
    /** Drop of audio enabled in file chooser */
    public final static int AUDIO = 1;
    /** Drop of video enabled in file chooser */
    public final static int VIDEO = 1;
    
   // Maximum number of recent files.            
   private static final int MAX_FILES = 8; 
   
   /* The following are gui objects used in the application. */
   private static JLabel       outputText;   // GUI label for output.
   private static JScrollPane  lessonScroll; // ScrollPane holding lesson list
   private static JPanel       lessonData;   // The panel to hold lesson data

   private static JPanel       lessons;        // Panel holding lesson list.
   private static JSplitPane   splitPane;      // Split pane in the main frame.
   private static JScrollPane  dataScrollPane; // Scroll pane for lesson data.
   private static Container    frame;          // Main application frame.

   // Recently used files and last used paths to different types of files.
   private static String[]   recentFiles;  // Array of recently open files.
   
   // File menu for recent files.
   private static FileMenu     recentFileMenu;
   
   private transient JFileChooser fc = null;
   private transient FileDialog  fd = null;
   
   /**
    *  Create the application run time environment 
    */
   public AppEnv(String[] args)  throws FileNotFoundException
   {
       Environment.setEnvironment(this, args);
       
       // Initialize the Files Object.
       Files.startUp();  
   }
   
   // Constructors to enable drag and drop into file choosers
   private AppEnv(JFileChooser fc) { this.fc = fc; fd = null; }
   private AppEnv(FileDialog fd)   { this.fd = fd; fc = null; }
   
   /**
    * <p>Method to close all open files and write the system parameters to disk</p>
    */
   public static boolean shutDown() 
   {
       if (!Files.closeAllFiles()) return false;
       getEnvironment().writeStatus(); 
  
       // Get property change listener maintaining file properties.
       PropertyChangeListener[] pcl =  
       Toolkit.getDefaultToolkit().getPropertyChangeListeners("Properties");
       SoundProperties soundProperties = (SoundProperties)pcl[0];
       String pathName = soundProperties.getPaths();
       String[] paths  = pathName.split(";");
       SoundDefaults.writeSoundDefaults(paths);  
       SoundData.resetSoundData();
       return true; 
   }
  
    //-----------------------------------------------------------------
      // Methods to get panels for managing the environment.
    //-----------------------------------------------------------------
    private static JLabel  getOutputLabel()
    {   if (outputText==null)  outputText = new JLabel("");
        return outputText;
    }
    
    /**
     *   <p>Method to get the panel that holds all of the lessons and the label
     *   that displays feedback messages.</p>
     */
      public static JScrollPane  getLessonScroll()
    {
        if (lessonScroll==null)
        {
           lessonScroll = new JScrollPane();
           lessonScroll.getVerticalScrollBar()
                        .setUnitIncrement(SCROLL_INCREMENT);
           lessonScroll.setPreferredSize(new Dimension(LESSON_WIDTH, HEIGHT));
           lessonScroll.setSize(lessons.getPreferredSize());
        }
        return lessonScroll;
    }
    
    /**
     *  <p>The scroll pane that holds the lesson panel</p>
     *
     */
    public static JPanel getLessonScrollPanel()
    {   if (lessons==null)
        { lessons = new JPanel();
          lessons.setLayout(new BoxLayout(lessons, BoxLayout.Y_AXIS));
          lessons.add(new PopUpButton
                      (LanguageText.getMessage("acornsApplication", 50)));
          lessons.add(getLessonScroll());
          lessons.add(new DirectionPanel());
          lessons.setVisible(false);
        }
        return lessons;
    }
    
    /**
     * Get the scroll pane that is in the data panel.
     */
    public static JScrollPane getDataScrollPane()
    {   if (dataScrollPane==null)  
        { dataScrollPane = new JScrollPane();
          dataScrollPane.getVerticalScrollBar()
                        .setUnitIncrement(SCROLL_INCREMENT);
          dataScrollPane.getHorizontalScrollBar()
                        .setUnitIncrement(SCROLL_INCREMENT);
        }
        return dataScrollPane;
    }
   
    /**
     *  Get application split pane
     */
    public static JSplitPane getSplitPane()    
    { 
       if (splitPane==null)
       {  splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT
                      , getLessonScrollPanel(), getDataPanel());
          splitPane.setDividerLocation(LESSON_WIDTH);
          splitPane.setOneTouchExpandable(true);
          splitPane.setPreferredSize(new Dimension(WIDTH, HEIGHT));
          BasicSplitPaneDivider spd = ((BasicSplitPaneUI)splitPane.getUI()).getDivider();
          spd.addMouseListener(new MouseAdapter()
		  {
			@Override
			public void mouseClicked(MouseEvent e)
			{
                int dividerLocation = splitPane.getLastDividerLocation();
                if (dividerLocation>LESSON_WIDTH - 10) 
                    splitPane.setDividerLocation(0);
                else
                    splitPane.setDividerLocation(LESSON_WIDTH);
          
                FileObject file = Environment.getActiveFile();
                if (file != null) { file.displayLessons();  }
 			}
		  });
          
          // Add listener to redisplay lesson when divider moves.
          ComponentAdapter cl = new ComponentAdapter()
          {  public @Override void componentMoved(ComponentEvent event)
             {
                FileObject file = Environment.getActiveFile();
                if (file != null) { file.displayLessons();  }
             }
          };
          spd.addComponentListener(cl);
       }
         return splitPane;   
    }
    
    
    /**
     *  <p>Method to hold lesson specific data</p>
     */
    public static JPanel     getDataPanel()
    {
        if (lessonData==null)
        {  lessonData = new JPanel();
           lessonData.setBackground(new Color(200,200,200));
           lessonData.setLayout(new GridBagLayout());  
        }
        return lessonData;
    }
    
     /**
    * <p>Method to get recently opened files</p>
    */
     public static String[] getRecentFiles() 
     { 
        String[] newRecentFiles = new String[MAX_FILES];
          File     file;
          int top = -1;
      
          // Purge files that don't exist.
          for (int i=0; i<MAX_FILES; i++)
          {
             if (recentFiles[i] != null)
               {
                file = new File(recentFiles[i]);
                  if (file.exists()) newRecentFiles[++top] = recentFiles[i];
               }
          }
          recentFiles = newRecentFiles;
        return recentFiles;
   }

   /**
    * <p>Method to add the path of a file just opened to the list of recently
    * opened files</p>
    *
    * @param name The qualified name of the path to the recently opened file
    */
   public static void addRecentFile(String name)
     {
        String[] newRecentFiles = new String[MAX_FILES];
        int      top=0, i=0;
     
        // Place new file in the front and copy non duplicates to new list.
        newRecentFiles[top++] = name;
        while (i<MAX_FILES && top<MAX_FILES)
        {
           if (recentFiles[i]!=null && !recentFiles[i].equals(name)) 
                 newRecentFiles[top++] = recentFiles[i];
             i++;
        }
        recentFiles = newRecentFiles;
        recentFileMenu.addFileItems();
     }
   
   /** Method to set the menu with recently opened files for dynamic update
    * 
    * @param recentFileMenu The recent FileMenu object
    */
   public static void setRecentFileMenu(FileMenu recentFileMenu)
   {
       AppEnv.recentFileMenu = recentFileMenu;
   }
 
   //------------------------------------------------------------------------
   //   Method to read the list of recently open files and other enviromnent
   //   data.  It reads from the .acorn_settings directory created in the 
   //   user home directory.
   //------------------------------------------------------------------------
   private static String oldJarDirectory;

   public static void readStatus()
   {  
      ObjectInputStream ois = null;
      
      // Create directories if they don't exist.
      SoundDefaults.getDataFolder(); // Documents
      
      // Path to user home/acorns for application specific data
      String dirName = SoundDefaults.getHomeDirectory();
      String maps = dirName + File.separator + "keymaps";
      KeyboardFonts.readFonts(maps);
      
      // Path to default place to load and save lessons
      String defaultPath = SoundDefaults.getDataFolder();

      // Read keyboard layout fonts
      
      // Set the blank layer name in case the read fails
      layerNames = new String[AcornsProperties.MAX_LAYERS];
      for (int i=0; i<layerNames.length; i++)
      {  layerNames[i] = ""; }

      // Initialize the recent files in case read fails
      recentFiles = new String[MAX_FILES];
      options     = new boolean[AcornsProperties.MAX_OPTIONS];
      for (int i=0; i<AcornsProperties.MAX_OPTIONS; i++) options[i] = true;
      
      // Reset media path name in case read fails
      pathNames = new String[AcornsProperties.TYPES];
      for (int i=0; i<pathNames.length; i++)  pathNames[i] = defaultPath;

      // Now read file path names.
      FileInputStream fis = null;
      try
      {         
         // Read settings file or create it.
         String settings = dirName + "/recent";
         fis     = new FileInputStream(settings);
         BufferedInputStream bis = new BufferedInputStream(fis);
         ois   = new ObjectInputStream(bis);
         
         recentFiles    = (String[])ois.readObject();
         
         pathNames       = (String[])ois.readObject();

         if (pathNames.length<AcornsProperties.PATHS)
         {
             String[] newPathNames = new String[AcornsProperties.PATHS];
             for (int i=0; i<newPathNames.length; i++)
             { newPathNames[i] = defaultPath;   }
             System.arraycopy(pathNames, 0, newPathNames, 0, pathNames.length);
             pathNames = newPathNames;
         }
         
         for (int i=0; i<pathNames.length; i++)
         {
              File openFile = new File(pathNames[i]);
              if (!openFile.exists()) {   pathNames[i] = defaultPath;  }
         }
         options = (boolean[])ois.readObject();
         
         try
         {    // Upwards compatibility. 
              ois.readObject();
              
              Integer score = (Integer)ois.readObject();
              Score.setDifficultyLevel(score.intValue());
         }
         catch (Exception e) {}
         try
         {
             Environment.saveFeedback
                     ((PicturesSoundData[])ois.readObject());
             oldJarDirectory = (String)ois.readObject();
             layerNames = (String[])ois.readObject();
         }
         catch (Exception e) {}
      }
      catch (Exception ioe) {}
      try {ois.close(); fis.close(); }catch (Exception ex) {}
   }  //End of readStatus  
   
   //--------------------------------------------------------------------------
   // Method to write the list of recently open files.  It writes to
   //   the .acorn_settings directory created in the user home directory.
   //--------------------------------------------------------------------------
   public void writeStatus()
   { 
	   try
       {      
         String dirName = SoundDefaults.getHomeDirectory();
         String maps = dirName + "/keymaps";
         KeyboardFonts.writeFonts(maps);
        
         String settings = dirName + "/recent";
         FileOutputStream     fos = new FileOutputStream(settings);
         BufferedOutputStream bos = new BufferedOutputStream(fos);
         ObjectOutputStream   oos = new ObjectOutputStream(bos);
            
         oos.writeObject(recentFiles);
         oos.writeObject(pathNames);
         oos.writeObject(options);
         
         String language = KeyboardFonts.getLanguageFonts().getLanguage(); 
         if (language==null) language = "English";
         oos.writeObject(language);
         
         oos.writeObject(Score.getDifficultyLevel());
         oos.writeObject(Environment.getFeedback());
         oos.writeObject(oldJarDirectory);
         oos.writeObject(layerNames);
         oos.close();
      }
      catch (Exception exception)
      {  recentFiles = new String[MAX_FILES];
      }
   }  //End of writeStatus
  
  /******** Filled in Polymorphic methods **************/ 
  /**
    *  <p>Method to get the primary application frame</p>
    */
   public JFrame getApplicationFrame()        
   {  if (frame==null)
      {
         // Instantiate the JFrame and set title and icon.
         JFrame newFrame = new JFrame(getTitle());
         newFrame.setIconImage
               (getIcon(AcornsProperties.ACORN ,20).getImage());      

         // Set the frames layout.
         newFrame.setLayout(new BorderLayout());
         newFrame.setBackground(new Color(200,200,200));
         newFrame.add(getSplitPane(), BorderLayout.CENTER);
         newFrame.add(getOutputLabel(), BorderLayout.SOUTH);
         newFrame.setFocusable(true);
         
         DropTarget target = new DropTarget(newFrame, new FrameDropTargetListener());
         newFrame.setDropTarget(target);

         // Set the frame variable and handle resizing.
         frame = newFrame;
         String title = LanguageText.getMessage("commonHelpSets", 85);
         UIManager.put("ProgressMonitor.progressText", title);
         
         ComponentAdapter componentAdapter = new ComponentAdapter()
         {
           /** Time to wait */
        	  private final int DELAY = 250;
        	  /** Waiting timer */
        	  private javax.swing.Timer waitingTimer;
        
        	  public @Override void componentResized(ComponentEvent e)
              { 
            	  if (this.waitingTimer==null)
            	  {
            	    /* Start waiting for DELAY to elapse. */
            	    waitingTimer = new Timer(DELAY,
            	    		new ActionListener()
            	    		{
		            	    	public void actionPerformed(ActionEvent event)
		            	    	{
		            	    		Timer timer = (Timer)event.getSource();
		            	    	    timer.stop();
		            	    	    timer = null;

		                            FileObject file = Environment.getActiveFile();
		                            if (file != null && file.getActiveLesson() != null) 
		                                file.displayLessons();
		                            
	                            	splitPane.setDividerLocation(LESSON_WIDTH);
		            	    	}
            	    		});
            	    waitingTimer.start();
            	  }
            	  else
            	  {
            	    /* Event came too soon, swallow it by resetting the timer.. */
            	    this.waitingTimer.restart();
            	  }
              }
            };  // End of anonymous action listener.

          frame.addComponentListener(componentAdapter);
          
          // Attach the property change listener for the sound listener.
          String[] paths = SoundDefaults.readSoundDefaults();                        
       
          // Set the property change listener for loading/saving files.
          if (paths==null || paths.length==0) 
          {  paths = new String[2];
             paths[0] = paths[1] = "";
          }
          String loadPath = paths[0];
          String savePath = "";
          if (paths.length>=2) savePath = paths[1];

          new SoundProperties(loadPath, savePath);
      }
      JOptionPane.setRootFrame((JFrame)frame);

      return (JFrame)frame; 
   }

   /** Reset the folder paths to a default value */
   public static void resetPaths(String defaultPath)
   {
	      pathNames = new String[AcornsProperties.TYPES];
	      for (int i=0; i<pathNames.length; i++)  pathNames[i] = defaultPath;
   }

 /**
    * <p>Method to display a text string to the GUI message label that 
    * shows at the south portion of the lessonPanel</p>
    *
    * @param message The message to display
    */
   public void setMsg(String message)  
   { getOutputLabel().setText(message); }
   
   /** Get size of data display area.
    *  @return data area dimension
    */
   public Dimension getDisplay()  
   {  if (Files.getMode())  return getScreenSize();

      Dimension frameSize = getFrame().getContentPane().getSize();
      int divider = getSplitPane().getDividerLocation();
      return new Dimension
              (frameSize.width - divider// - FRAME_BORDER 
                  , frameSize.height-CONTROL_HEIGHT - FRAME_BORDER);
   }
 
   /** Get Screen Size */
   public Dimension getScreen()
   {   if (!Files.getMode()) 
       {
           GraphicsEnvironment ge 
                   = GraphicsEnvironment.getLocalGraphicsEnvironment();
           Rectangle bounds = ge.getMaximumWindowBounds();
           return new Dimension(bounds.width-bounds.x
                              , bounds.height-bounds.y);
       }
       else    
       {
           JFrame play = (JFrame)getPlayFrame();
           return play.getSize();
       }
   }
   
   /** Enable appropriate menus. */
   public void enable() { AcornMenu.setEnable(); }

   /** Method to determine if this is an Applet.
    */
   public boolean ifApplet() {  return false; }
  
   public static File chooseFile(String title, int option, DialogFilter dialog, String defaultName, JPanel accessory)
   { 
	   return AppEnv.chooseFile(title, option, dialog, defaultName, accessory, NONE);
   }
   
   /** Configure the file chooser to be compatible with both MacOS and Windows
    * 
    * @param title The text to appear at the top of the dialog
    * @param option AcornsProperties.PICTURES, SOUNDS, OPEN, SAVE, or VIDEO
    * @param dialog The appropriate file filter
    * @param accessory An Accessory panel to display
    * @param default file name
    * @param drop = NONE, AUDIO, PICTURE, NONE
    * @return Selected File object or null if cancelled
    */
   public static File chooseFile(String title, int option, DialogFilter dialog, String defaultName, JPanel accessory, int drop)
   { 
	   
	   File file = null;
	   String path = AppEnv.getPath(option);
	   int dialogOption;

	   String osName = System.getProperty("os.name");
       if (!osName.contains("Mac")) 
       {  
     	   final JFileChooser fc = new JFileChooser(path);
           fc.setDialogTitle(title);
           fc.setAcceptAllFileFilterUsed(false);

           if (dialog!=null) fc.addChoosableFileFilter(dialog);
           if (accessory!=null) fc.setAccessory(accessory);
           if (defaultName!=null) fc.setSelectedFile(new File(defaultName));
           
           if (drop==VIDEO)
        	   new VideoDropTarget(fc, new AppEnv(fc));
 
           int returnVal; 
     	   if (option == AcornsProperties.SAVE)
     		  returnVal = fc.showSaveDialog(AppEnv.getFrame());
     	   else
               returnVal = fc.showOpenDialog(AppEnv.getFrame());

     	   if (returnVal == JFileChooser.APPROVE_OPTION)
           {
               file = fc.getSelectedFile();
           }
      }
      else
      {   
    	  Frame frame = AppEnv.getRootFrame();

    	  dialogOption = FileDialog.SAVE;
    	  if (option != AcornsProperties.SAVE)
    		  dialogOption = FileDialog.LOAD;
    	  
          FileDialog fd = new FileDialog(new Dialog(frame), title, dialogOption);
          
          fd.setDirectory("");
          if (dialogOption != FileDialog.SAVE)
        	  fd.setDirectory(path);
          
          if (dialog!=null) fd.setFilenameFilter(dialog);
          if (defaultName!=null) fd.setFile(new File(defaultName).getName());
          if (drop==VIDEO) 
        	  new VideoDropTarget(fd, new AppEnv(fd));

          fd.setVisible(true);
          
          String fileName = fd.getFile();
          String directory = fd.getDirectory();
          
          if (fileName != null && fileName.length()!=0)
          {
              file = new File(directory + fileName);
          }
      }
      
      return file;
   }

   /** Handle dropping a video into the choose file dialog */
   public void videoDropped(File file)  
   {  
	   if (fc!=null)
		   fc.setSelectedFile(file);
	   else if (fd!=null)
	   {
		   fd.setDirectory(file.getParent());
		   fd.setFile(file.getName());
	   }
   }
	 

   /** Method to get a picture using a fileChooser dialog
    *
    * @return The file object that references the picture
    */
   public URL getPicture() throws IOException
   {
	   String title = LanguageText.getMessage("acornsApplication", 51);
	   int option = AcornsProperties.PICTURES;
       String[] imageArray = ImageIO.getReaderFormatNames();
       DialogFilter dialogFilter = new DialogFilter("Image Files",imageArray);
    	  
    	  
      File file = chooseFile(title, option, dialogFilter, null, null);
      if (file !=null)
      {   
          if (!file.exists()) return null;
      }
      else  return null;

      // Open the image file and check if the size is valid.
      String fileName = file.getCanonicalPath();
      String name = file.getName();
      int lastIndex = fileName.lastIndexOf(name);
      if (lastIndex<0) return null;

	  String path = fileName.substring(0,lastIndex-1);
	  AppEnv.setPath(AcornsProperties.PICTURES, path);

      return file.toURI().toURL();
   }
   
   
}  // End of AppEnv
