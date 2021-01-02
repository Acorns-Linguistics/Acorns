/*
 * EditMovietoAnimation.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.LayoutManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import javax.swing.JFrame;
//import javax.swing.JOptionPane;

import org.acorns.Environment;
import org.acorns.language.LanguageText;
import org.acorns.lesson.AcornsProperties;
import org.acorns.lib.DialogFilter;
import org.acorns.visual.GifEncoder;

public class EditMovietoAnimation extends MenuOption
{
    private Container     contentPane;
    private JFrame        playFrame;
    private WindowExit    exit;
    private LayoutManager originalLayout;
    private static MediaJFXPanel panelVideo;
    
    private static Exception exception = null;
    
    // Method called from AcornMenu.
    public String processOption(String[] args)
    {
       Platform.setImplicitExit(false);
       playFrame = (JFrame)AppEnv.getPlayFrame();
       contentPane = playFrame.getContentPane();
       originalLayout = contentPane.getLayout();

       String file  = null;
       try
       {
    	   file = openFile();
       }
       catch (FileNotFoundException e)
       {
    	   return e.getMessage();
       }
       catch (IOException e)
       {
    	   return e.toString();
       }
       
       contentPane.removeAll();
       contentPane.setLayout(new BorderLayout());
       exit = new WindowExit();
       playFrame.addWindowListener( exit );
       playFrame.invalidate();

       try  
       {  
    	   if (exception == null)
    	   {
       		   panelVideo = new MediaJFXPanel(file);      		   
    	   } 
    	   else throw new RuntimeException(exception.getMessage());
       }
       catch (RuntimeException e)
       {
    	   exception = e;
    	   return e.toString();
       }
       catch (Exception e)
       {  
    	  if (panelVideo != null) exit.shutdown(); 
          return LanguageText.getMessage("acornsApplication", 149);
       }
       
       contentPane.add(panelVideo, BorderLayout.CENTER);

       playFrame.setVisible(true);
       playFrame.setLocationRelativeTo(null);
       Environment.getFrame().setVisible(false);

       Files.setMode(true);
       return LanguageText.getMessage("acornsApplication", 153);
    }

    /** Method to access a video file */
    private String openFile () 
    		throws FileNotFoundException, IOException
    {
       // Standard open file option.
       File file;

       int option = AcornsProperties.VIDEO;
       String title = LanguageText.getMessage("acornsApplication", 90);
       String[] extensions
               = {"avi", "AVI", "mov", "MOV", "mpeg", "MPEG"
                       , "mp4", "mp1", "mp2", "mp3", "m4v", "mpg", "MPG"};
       final DialogFilter dialog = new DialogFilter("Video Files", extensions);
       

       file = AppEnv.chooseFile(title, option, dialog, null, null, AppEnv.VIDEO);
       if (file != null)
       {  
          if (!file.exists())
              throw new FileNotFoundException(
              	LanguageText.getMessage
                  ("acornsApplication", 71, file.getName()));
       }
       else  
           throw new FileNotFoundException(
                 	LanguageText.getMessage
                     ("acornsApplication", 70));
 
       String path;

       String directory = path = file.getCanonicalPath();
       int lastIndex    = directory.lastIndexOf(file.getName());
       if (lastIndex<0)
          return LanguageText.getMessage("acornsApplication", 69);
       directory = directory.substring(0,lastIndex-1);
       AppEnv.setPath(AcornsProperties.VIDEO, directory);

       return path;
    }

    /** Class to shutdown the media player before closing the frame */
    public class WindowExit extends WindowAdapter
    {
        public @Override void windowClosing(WindowEvent ev) 
        { 
          if (panelVideo!=null)
          {   
        	  String msg = LanguageText.getMessage("acornsApplication", 158);
              Environment.setText(msg);
          }
          shutdown(); 
        }

        public void shutdown()
        {
        	if (panelVideo != null)
    		{
            	panelVideo.dispose();
    		}

            playFrame.removeWindowListener(this);
            contentPane.setLayout(originalLayout);
        }
    }

	/** Create JFXPanel to play and control the video */
	private class MediaJFXPanel extends JFXPanel implements EventHandler<ActionEvent>
	{
		private static final long serialVersionUID = 1L;
	    private static final int PREF_WIDTH = 100;
	    private static final int START=0, END=1, RESET=2, PLAY=3, SAVE=4;
	    private static final int MAX_CLIP=10;
	    private static final float CLIP_DELTA = 1/15F;
	    
	    private MediaPlayer  player;
	    private MediaView    view;
	    private MediaControl control;
	    private Scene scene;

	    String[] buttonText;
        Button[] buttons;
        Label[] labels;

        Duration startTime, endTime;
        Thread thread;
        
        DataOutputStream out;
        ArrayList<BufferedImage> images;
        
	    
	    /** Constructor: 
	     *    Instantiate panel with Media Player and controls
	     *    
	     *   @param file The video file   
	     */    
		public MediaJFXPanel(final String file) throws FileNotFoundException, MediaException, RuntimeException
		{
			// Create components needed for making gif files
		    buttonText = LanguageText.getMessageList("commonHelpSets", 72);
	        buttons = new Button[buttonText.length];
	        labels = new Label[2];
	        startTime = endTime = null;
	        thread = null;
     	    Scene scene = createScene(file);
	        
	        Platform.runLater(new Runnable() 
	        {
	            @Override
	            public void run() 
	            {
	               try
	               {
	            	   setScene(scene);
	               }
	               catch (Exception e)
	               {
	            	   popupMessage(e.toString());
	               }
	            }
	       });
		}
		
		private void setVideo(String file) throws FileNotFoundException
		{
	        // create media player
	        String fileUri = new File(file).toURI().toString();
	        
	        Media media;
	        try
	        {
		        media = new Media (fileUri);
		        player = new MediaPlayer(media);
	        }
	        catch (MediaException e) 
	        {
	        	Label label = new Label(e.toString());
	        	label.setAlignment(Pos.CENTER);
	        	label.setFont(new Font("Verdana", 12));
	        	label.setTextFill(Color.RED);
	        	scene.setRoot(label);
	        }

	        player.setAutoPlay(false);
	        player.setCycleCount(0);
	        //player.setMute(true);
	    	
	        HBox gifControls = makeGifControls();
	        control = new MediaControl(player);
	    	control.setTop(gifControls);
	
	        view = new MediaView(player);
	        view.setFitWidth(800);
	        view.setFitHeight(600);
	    	control.setCenter(view);
	    	scene.setRoot(control);
	    	
	    	player.setOnPaused(new Runnable() {

				@Override
				public void run() 
				{
					player.setStopTime(player.getMedia().getDuration());
				} });
	        
		}
	
		/** Create the scene for the JFXPanel
		 * 
		 * @param file The video file to display
		 * @return The created scene
		 */
	    private Scene createScene(String file) throws FileNotFoundException
	    {
	        Group  root  =  new  Group();
	        scene  =  new  Scene(root, Color.ALICEBLUE);
	        scene.widthProperty().addListener(new ChangeListener<Number>() 
	        {
	            @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) 
	            {
	            	stopOperation();
	            }
	
		    });
	        
	        scene.heightProperty().addListener(new ChangeListener<Number>() 
	        {
	            @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) 
	            {	
	            	stopOperation();
	            }
	        });
	        
	        // create media player
	        setVideo(file);
	        return scene;
	    }
	    
	    /** Make panel with controls for creating a gif from a video
	     * 
	     * @return THe HBox panel containing controls
	     */
	    private HBox makeGifControls()
	    {
	    	HBox buttonPanel = new HBox();
	    	buttonPanel.setAlignment(Pos.CENTER);
	    	buttonPanel.setSpacing(10);
	    	buttonPanel.setPadding(new Insets(0, 20, 10, 20)); 
	    	ObservableList<Node> list = buttonPanel.getChildren();
	
	    	for (int i=0; i<buttons.length; i++)
	    	{
	    		buttons[i] = new Button(buttonText[i]);
	    		list.add(buttons[i]);
	    		if (i<2)
	    		{
	    			labels[i] = new Label();
	    			labels[i].setPrefWidth(PREF_WIDTH);
	    			list.add(labels[i]);
	    			HBox.setHgrow(labels[i], Priority.ALWAYS);
	    		}
	    		else
	    		{
	    			HBox.setHgrow(buttons[i], Priority.ALWAYS);
	    		}
	    		buttons[i].setOnAction(this);
	    	}
    		resetClipTime();
    		endTime = null;
	    	return buttonPanel;
	    }
	
	    /** Listener method to respond to control buttons */
		@Override public void handle(ActionEvent event) 
		{
			Button button = (Button)event.getSource();
			stopOperation();
			String error, text = button.getText();
			
			if (text.equals(buttonText[START]))
			{
				startTime =  player.getCurrentTime();
				
			    labels[0].setText(formatTime(startTime));
			}
			else if (text.equals(buttonText[END]))
			{
				endTime = player.getCurrentTime();
		    	labels[1].setText(formatTime(endTime));
			}
			else if (text.equals(buttonText[RESET]))
			{
				resetClipTime();
			}
			else if (text.equals(buttonText[PLAY]))
			{
				error = validateClip(startTime, endTime, false);
				if (error==null)
				{
			       	Platform.runLater(new Runnable() 
			        {
			            @Override
			            public void run() 
			            {
		                    player.seek(startTime);
		                    player.setStopTime(endTime);
			                player.play();
			            }
			        });
				}
				else
				{
					popupMessage(error);
				}
			}
			else if (text.equals(buttonText[SAVE]))
			{
				error = validateClip(startTime, endTime, true);
				if (error==null)
				{
                    File file = selectGifFileName();
                    if (file==null) return;
                    
                    try
                    {
	                    startOperation(file);
                    }
                    catch (FileNotFoundException e)
                    {
                    	popupMessage(LanguageText.getMessage("commonHelpSets", 65));
                    }
				}
				else
				{
					popupMessage(error);
				}
			}
			
		}	// End of handle method

		/** Format the duration from start to hh:mm:ss
		 * 
		 * @param elapsed The elapsed time object
		 * @return String formatting the elapsed time
		 */
		private String formatTime(Duration elapsed)
		{
			if (elapsed==null) return "00:00:00";
			
			String hours = "0" + (int)elapsed.toHours();
			String minutes = "0" + (int)elapsed.toMinutes() / 60;
			String seconds = "0" + (int)elapsed.toSeconds() % 60;
			
			hours = hours.substring(hours.length()-2);
			minutes = minutes.substring(hours.length()-2);
			seconds = seconds.substring(hours.length()-2);

			
			String result = String.format("%s:%s:%s", hours, minutes, seconds);
			return result;
		}
		
	    /** Method to reset the beginning and ending time for a video clip */
	    private void resetClipTime()
	    {   
	    	startTime = new Duration(0);
	    	player.setStartTime(startTime);
	    	labels[0].setText(formatTime(startTime));
	    	
	    	endTime = player.getMedia().getDuration();
	    	player.setStopTime(endTime);
	    	labels[1].setText(formatTime(endTime));
	    }

	    /** Create an analog to JOptionPne in JavaFX 
	     * 
	     * @param message The text to display as a message.
	     */
	    private void popupMessage(String message)
	    {
	    	Alert alert = new Alert(AlertType.WARNING);
	    	String title = LanguageText.getMessage("acornsApplication", 163);
	    	alert.setTitle(title);
	    	alert.setHeaderText(message);
	    	alert.showAndWait();
	    }


	    /** Check if clip parameters are valid
	     * 
	     *  @return null if clip is valid, error message otherwise
	     *  
	     */
	    private String validateClip(Duration first, Duration last, boolean saving)
	    {
	    	// Verify selected times
	    	if ((first == null || last == null)
	    		|| (first.greaterThanOrEqualTo(last))
	    		|| (last.greaterThan(player.getMedia().getDuration()))
	    		|| (first.lessThan(new Duration(0)))
	    	)
	    	{
		    	return LanguageText.getMessage("commonHelpSets", 73);
	    	}

	    	// Duration check only applies if writing to GIF
    		if (saving && ( (last.toSeconds() - first.toSeconds()) > MAX_CLIP))
    		{
		        return LanguageText.getMessage("commonHelpSets", 74);
    		}
    		return null;

 	    }  // End isValidClip()

	    /** Method to get a picture using a file chooser dialog
	    *
	    * @return The file object that references the picture
	    */
	    public File selectGifFileName()
	    {
	       String name = null, directory;
	       AcornsProperties properties = AcornsProperties.getAcornsProperties();

	       String title = LanguageText.getMessage("acornsApplication", 38);
	       int option = AcornsProperties.SAVE;
	       
	       String[] imageArray = {"gif"};
	       DialogFilter dialogFilter = new DialogFilter("Image Files",imageArray);

	       File file = AppEnv.chooseFile(title, option, dialogFilter, null, null);
	       if (file != null)
	       {   
	           try 
	           { 
	        	   name = file.getCanonicalPath(); 
	           }
	           catch (Exception e) { return null; }
	           
	           if (name!=null && !name.endsWith(".gif")) name += ".gif";
	           file = new File(name);
	       }
	       else  return null;

	       int lastIndex    = name.lastIndexOf(file.getName());
	       if (lastIndex>=0)
	       {  directory = name.substring(0,lastIndex-1);
	          if (properties != null) 
	        	  properties.setPath(AcornsProperties.PICTURES, directory);
	       }
	       return file;
	    }
	    
	    public void dispose()
	    {
	    	stopOperation();
	    	System.gc();
	    }

        /** Stop current playback
         * 
         * @return
         */
        public void stopOperation()
        {
        	if (thread != null)
        	{
        		if (thread.isAlive()) 
        			thread.interrupt();
            
	            try
	            {
	            	thread.join(1000);
	            }
	            catch (InterruptedException e)  {}
        	}
       		if (player!= null) 
       		{
       			try
       			{
       				player.pause();
       			}
       			catch (Exception e) {};
       		}
        }
        
        /** Play to next frame to write 
         * 
         * @return false when finished playback
         */
        private void startOperation(final File file)
        					throws FileNotFoundException
        {
        	out = new DataOutputStream(
        			new FileOutputStream(file));
          
			thread = new Thread( new Runnable() 
			{
				@Override
				public void run() 
				{
					images = new ArrayList<BufferedImage>();
					
					player.seek(startTime);
					player.setStopTime(endTime);
					
					int frames = (int)((endTime.toSeconds() - startTime.toSeconds()) / CLIP_DELTA);
					for (int frame = 0; frame<frames; frame++)
					{
						player.pause();
				       	Platform.runLater(new Runnable() 
				        {
				            @Override
				            public void run() 
				            {
				               SnapshotParameters snapShotParams = new SnapshotParameters();
				           	   snapShotParams.setFill(Color.TRANSPARENT);
				           	   WritableImage image = view.snapshot(snapShotParams, null);
				           	   BufferedImage newImage = SwingFXUtils.fromFXImage(image, null);
				           	   images.add(newImage);
				            }
				        });

						try
						{
							Thread.sleep((int)(CLIP_DELTA * 1000));
						}
						catch (InterruptedException e)
						{
								break;
						}
						player.play();
					}
					
			       	Platform.runLater(new Runnable() 
			        {
			            @Override
			            public void run() 
			            {
							player.pause();
			            }
			        });
			       	
		        	GifEncoder encoder = new GifEncoder();
		 	        encoder.setFrameRate(1/CLIP_DELTA);
		 	        encoder.setRepeat(0);
		 	        encoder.start(out);
		 	        
		 	        for (int i=0; i<images.size(); i++)
		 	        {
		 	           encoder.setDelay((int)(CLIP_DELTA*1000));
		 	           encoder.addFrame(images.get(i));
		 	        }
		 	        
	                encoder.finish();
	                try {  out.close(); } 
	                catch (IOException e) {}

				}		// End of run()
				
			});
			
			thread.start();
        }		// End of startOperation()
	    
	} // End MediaJFXPanel class
	
}      // End of EditMovietoAnimation class
