/*
 * Environment.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
 package org.acorns;
 
 import java.awt.*;
 import java.awt.event.*;
 import javax.swing.*;

 import java.util.*;
 import java.io.*;
 import java.net.*;
 import javax.help.*;

 import org.acorns.data.*;
 import org.acorns.lesson.*;
 import org.acorns.audio.*;
import org.acorns.language.*;
 
 public abstract class Environment
 {  
    /** Default size of ICONS displayed by Acorns */
    public static final int ICON_SIZE = 20;
   
    /* ICON file names */
    private static final String[] icons 
          = {"acorn.png", "anchor.png", "record.png"
           , "play.png", "stop.png", "browse.png", "tick.png", "blank.png"
           , "rotate.png", "up.png", "down.png", "info.png", "begin.png"
           , "end.png", "prev.png", "next.png"
           , "zoomin.png", "zoomout.png", "help.png"
           , "close.png", "print.png", "background.png", "font.png"
           , "soundeditor.png", "web.png", "copy.png", "paste.png"
           , "left.png", "center.png", "image.png", "answers.png", "check.png"
           , "replay.png", "moveright.png", "moveleft.png", "random.png"
           , "pause.png", "slowdown.png", "speedup.png"};
			  
    /* Sound file names */
    private static final String[] sounds 
                         = {"correct.wav", "incorrect.wav", "spell.wav" };
    private static SoundData[] soundDataObject = new SoundData[sounds.length];

    protected static boolean[]    options;         // on/off flags
    private   static boolean      play;            // Play mode flag
    protected static FileObject   active;          // Active open file
    protected static Container    playFrame;       // Play mode frame
    protected static String[]     pathNames;       // Paths for file choosers
    protected static Environment  envObj;          // Environment Object
    protected static ArrayList<String> msgList;    // List of messages
    protected static PicturesSoundData[] feedback; // Feedback recordings
    protected static HelpSet  helpSet;              // System help object
    protected static String[] layerNames;           // Default layer names


    public static void setEnvironment(Environment env, String[] args) 
                                               throws FileNotFoundException
    {  active = null;
       playFrame = null;
       envObj = env;
       msgList = null;

       new KeyDispatcher();
      
       // Register the available lesson types.
       if (args!=null && !setAvailableLessonTypes(args))
           {  throw new FileNotFoundException(); }

       UIManager.LookAndFeelInfo[] looks = UIManager.getInstalledLookAndFeels();
       try
       {  for (int i=0; i<looks.length; i++)
          {  if (looks[i].getName().contains("Windows"))
             {  UIManager.setLookAndFeel(looks[i].getClassName());  }
          }
       }
       catch (Exception e) {}
    }
	   
    /**
     * Set the active file object
     */ 
     public static void setActiveFile(FileObject file)
     {  active = file; }

    /**
     *  Return the file object associated with the active file.
     */   
     public static FileObject getActiveFile()
     {  return active; }
    
    /** 
     * Method to return the title for the Acorns project
     */
     public static String getTitle() 
     { return LanguageText.getMessage("acornsApplication", 35); }

    /**
     * <p>Method to display a text string to the GUI message label that 
     * shows at the south portion of the lessonPanel</p>
     *
     * @param message The message to display
     */
    /** 
     * Mutator method to set play mode flag.
     */
     public static void setMode(boolean playMode)
     {  play = playMode; 
     }
   
    /**
     * Accessor method to return whether system is in set-up or execute mode
     * @return true if in play mode, false otherwise
     */
     public static boolean getMode() 
     {return play;}
     
     /**  Method to exit play mode
      *   @return true if successful, false otherwise
      */
     public static boolean exitPlayMode()
     {
         if (getMode() && !isApplet())
         {
            PlayBack.stopPlayBack();
            setMode(false);
            JFrame frame = (JFrame)getPlayFrame();
            Container container = frame.getContentPane();
            container.removeAll();

            getPlayFrame().setVisible(false);
            getFrame().setVisible(true);
    	       
            FileObject activeFile = getActiveFile();
            if (activeFile!=null) activeFile.displayLessons();
            return true;
         }
         return false;
     }
     
     public static Frame getRootFrame()
     {
    	 Frame frame = getFrame();
    	 if (frame.isVisible()) return frame;
    	 
    	 frame = (Frame)getPlayFrame();
    	 if (frame.isVisible()) return frame;
    	 
    	 frame = JOptionPane.getRootFrame();
    	 return frame;
     }
     
    /** Return the environment object */
     public static Environment getEnvironment() { return envObj; }
   
   /**
    * <p>Method to set the speech, spelling, and gloss system options</p>
    */
    public static void setOptions(boolean[] newOptions)
    {  options = newOptions; 
       if (isApplet()) envObj.writeStatus();
   }
   
   /**
    * <p>Method to get the speech, spelling, and gloss system options</p>
    */
    public static boolean[] getOptions()      
    {
       if (options==null)
       {   options = new boolean[AcornsProperties.MAX_OPTIONS];
             for (int i=0; i<AcornsProperties.MAX_OPTIONS; i++) options[i] = true;
       }
       
       if (options.length < AcornsProperties.MAX_OPTIONS)
       {
    	   boolean newOptions[] = new boolean[AcornsProperties.MAX_OPTIONS];
    	   for (int i=0; i<options.length; i++)  newOptions[i] = options[i];
    	   options = newOptions;
       }
       return options.clone();
    }
    
   /**
    * <p>Method to set file chooser path for opening and saving files.</p>
    *
    * @param type Index to the appropriate file chooser path.
    * @param path Path to the last file selection made by a user.
    *
    * <p>See: Status.PICTURES, Status.SOUNDS, Status.OPEN, and Status.SAVE</p>
    */
    public static void setPath(int type, String path)
    {  pathNames[type] = path;    }

   /**
    * <p>Method to get file chooser path for opening and saving files.<br /></p>
    *
    * @param type Index to the appropriate file chooser path.
    *
    * <p>See: Status.PICTURES, Status.SOUNDS, Status.OPEN, and Status.SAVE</p>
    */
    public static String getPath(int type) 
    {  return pathNames[type]; }
  
   /** Set the main frame. This method is called by the Web-player
    * @param thisFrame The Japplet object.
    */
    public static void setPlayFrame(Container thisFrame)
    {
       playFrame = thisFrame;
       setMode(true);
    }
   
   /**
    * Return frame for play mode.
    */
    public static Container  getPlayFrame()    
    { 
       Rectangle vga = new Rectangle(0, 0, 1024, 704);
       if (playFrame==null)
       {
          JFrame playFramePanel = new JFrame
           (getTitle() + " - " + LanguageText.getMessage("commonHelpSets", 68));
          playFramePanel.setIconImage(getIcon(AcornsProperties.ACORN ,20).getImage()); 
          playFramePanel.setResizable(true);
          JFrame.setDefaultLookAndFeelDecorated(true);
          playFramePanel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
          GraphicsEnvironment ge 
                     = GraphicsEnvironment.getLocalGraphicsEnvironment();
          Rectangle bounds = ge.getMaximumWindowBounds();
          if (bounds.width<vga.width)  vga.width = bounds.width;
          if (bounds.height < vga.height) vga.height = bounds.height;
          
          playFramePanel.setExtendedState(playFramePanel.getExtendedState()|Frame.MAXIMIZED_BOTH);
          playFramePanel.setMaximizedBounds(vga);
          playFramePanel.setPreferredSize(new  Dimension(vga.width, vga.height));
          playFramePanel.setSize(playFramePanel.getPreferredSize());
          playFrame = playFramePanel;
          playFramePanel.addWindowListener(new WindowAdapter() 
          {   public @Override void windowClosing(WindowEvent ev) 
              {     
                  Runtime runEnvironment = Runtime.getRuntime();
                  runEnvironment.gc();
                  // For debugging, we can print it to see how much left.
                  //long free = runEnvironment.freeMemory();
                  exitPlayMode();
              }
          });
          playFrame.setVisible(true);
          playFrame.setVisible(false);;
       }
       return playFrame;   
    }

    /** Determine the list of available lesson types 
     *  @return false if none available.
     */
     public static boolean setAvailableLessonTypes(String[] args)
     {  return LessonTypes.setAvailableLessonTypes(args);  }
         
    /** <p>Method to retrieve an acorn for display</p>
     *  
     * @param which which icon to display
     * @param size icon size (use default if size <=0)
     */
     public static ImageIcon getIcon(int which, int size)
     {    if (size<=0) size = ICON_SIZE;
          
          String iconName = "/data/" + icons[which];
          ImageIcon image = null;
          try
            {
               URL url = envObj.getClass().getResource(iconName);
               Image newImage  = Toolkit.getDefaultToolkit().getImage(url);
                 newImage = newImage.getScaledInstance
                                   (size, size, Image.SCALE_REPLICATE);
                 image = new ImageIcon(newImage);
            }
            catch (Exception e)
            {  JOptionPane.showMessageDialog
                       (null, LanguageText.getMessage("acornsApplication", 34)
                                                                + " " + iconName);
               System.exit(1);
            }
            return image;
     }
     
    /** Method to create a SoundData object from known files.
     *  @param which Which known file to read
     *  @return SoundData object or null if read fails.
     */
     public static SoundData getSound(int which)
     {
        SoundData sound;

        if (soundDataObject[which]!=null) return soundDataObject[which];
        Vector<SoundData> vector = null;
        if (feedback!=null)
        {
           vector = feedback[which].getVector();
        }

        if (feedback==null || vector==null || vector.isEmpty())
        {
           String soundName = "/data/" + sounds[which];
           URL url = envObj.getClass().getResource(soundName);
           sound = new SoundData();
           try  { sound.readFile(url); }
           catch (Exception e) { sound = new SoundData(); }
        }
        else
        {
           int index = (int)(Math.random() * vector.size());
           sound = vector.get(index);
        }
        return sound;
     }
     
     /** Get SoundData vector for a particular feedback (correct, incorrect, or close)
      * 
      * @param which AcornsProperties.CORRECT, AcornsProperties.INCORRECT, AcornsProperties.SPELL
      * @return vector of SoundData objects
      */
     public static Vector<SoundData> getFeedbackVector(int which)
     {
    	 if (feedback==null) return null;    	 
    	 return feedback[which].getVector();
     }
     
   /** Method to get the ACORNS help set */
    public static HelpSet getHelpSet()
    {
       if (helpSet!=null) return helpSet;
       try
       {
          URL helpURL = Environment.class.getResource("/helpData/acorns.hs" );
          if (helpURL==null) throw new Exception();

          ClassLoader loader = Environment.class.getClassLoader();
 	      helpSet = new HelpSet(loader, helpURL);
          return helpSet;
       }
       catch (Throwable t) {}
       return null;
    }

   // Various methods defined in the child class.
   public static void setText(String message)  
   { envObj.setMsg(message);  } 
	
	public static Dimension getDisplaySize() 
	{ return (Dimension)envObj.getDisplay(); }
	
	public static Dimension getScreenSize()
	{  return (Dimension)envObj.getScreen(); }
	
	public static boolean isApplet() 
	{  return envObj.ifApplet();  }
	
	public static JFrame getFrame()
	{  return envObj.getApplicationFrame(); }
	
	public static void setEnable()	{ envObj.enable(); }

     /** Method to return the working directory */
     public static String getWorkingDirectory()
     {   String workingDir =  System.getProperty("user.dir");
         return workingDir;
     }
     
     /** Method to save the array of feedback recordings
      *
      * @param feedback Array of feedback recordings
      */
     public static void saveFeedback(PicturesSoundData[] feedback)
     {  Environment.feedback = feedback;  }

     /** Method to load the array of feedback recordings
      *
      * @return The array of feedback recordings
      */
     public static PicturesSoundData[] getFeedback()
     {   if (feedback==null)
         {  if (feedback==null)
            {   feedback = new PicturesSoundData[3];
                for (int i=0; i<feedback.length; i++)
                {  feedback[i] = new PicturesSoundData();   }
            }
         }
         PicturesSoundData[] newFeedback  = new PicturesSoundData[feedback.length];
         for (int i=0; i<feedback.length; i++) newFeedback[i] = feedback[i].clone();
         return newFeedback;
     }
     
   /** Method to get the default names for each lesson layer */
   public static String[] getDefaultLayerNames() { return layerNames; }
   
   /** Method to set the default names for each lesson layer
    * 
    * @param layerNames Array of layer names
    */
   public static void setDefaultLayerNames(String[] layerNames) 
   {  Environment.layerNames = layerNames; }


     // Abstract methods.
     public abstract void setMsg(String message);
     public abstract Dimension getDisplay();
     public abstract Dimension getScreen();
     public abstract JFrame getApplicationFrame();
     public abstract URL getPicture() throws IOException;
     public abstract boolean ifApplet();
     public abstract void enable();
     public abstract void writeStatus();
      
}  // End of Environment class.
 