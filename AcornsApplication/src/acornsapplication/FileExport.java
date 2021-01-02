/*
 * FileExport.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.io.*;
import java.awt.*;

import javax.swing.*;

import java.awt.event.*;

import javax.imageio.*;

import org.acorns.*;
import org.acorns.audio.*;
import org.acorns.lesson.*;
import org.acorns.lib.*;
import org.acorns.language.*;

//----------------------------------------------------------
// Class to export a file with a selected name.
//----------------------------------------------------------
public class FileExport extends MenuOption
{
   // Variables needed by inner listener classes.
   private int      soundSelect,     imageSelect;
   private String[] soundExtensions, imageExtensions;
   private int      index;
   
	//----------------------------------------------------------
	// Method to process export options.
	//----------------------------------------------------------
   public String processOption(String[] args)
   {
      JRadioButton button;     // Temporary radio button component
      File         file;       // File object for selected export file
	  File         directoryF; // File directory object for lesson
	  String       fullName;   // Name of selected export file name.

      String[] msgs = LanguageText.getMessageList("acornsApplication", 102);
		
      // Get active file.
      AppObject active = Files.getActiveFile();
		
      // Create group of buttons for the types of sound files.    
      JPanel soundButtons = new JPanel();
      BoxLayout box = new BoxLayout(soundButtons, BoxLayout.Y_AXIS);
      soundButtons.setLayout(box);
      soundButtons.setBorder(BorderFactory.createEtchedBorder());
      ButtonGroup soundGroup = new ButtonGroup();
      
      soundButtons.add(new JLabel(msgs[0]));
      soundExtensions = SoundDefaults.getAudioWriterExtensions();
      String audioDefault = "mp3";
      for (index=0; index<soundExtensions.length; index++)
      {
         if (soundExtensions[index].equals(audioDefault))
         {   soundSelect = index;
             button = new JRadioButton(soundExtensions[index], true);
         }
         else button = new JRadioButton(soundExtensions[index]);
         button.addActionListener(
            new ActionListener()
            {  int selection = index;
            
               public void actionPerformed(ActionEvent event)
               {   soundSelect = selection;   }
            });
         soundButtons.add(button);
         soundGroup.add(button);
      }
      
      // Create group of buttons for the types of image files.
      button = null;
      JPanel imageButtons = new JPanel();
      box = new BoxLayout(imageButtons, BoxLayout.Y_AXIS);
      imageButtons.setLayout(box);
      imageButtons.setBorder(BorderFactory.createEtchedBorder());
      ButtonGroup imageGroup = new ButtonGroup();
      
      imageButtons.add(new JLabel(msgs[1]));
      imageExtensions = ImageIO.getWriterFormatNames();
      imageSelect = 0;
      String[] imageImports = ImageIO.getReaderFormatNames();

      // Make sure that only those formats readable and writeable are selected
      java.util.Vector<String> extensions = new java.util.Vector<String>();
      extensions.add(msgs[2]);
      boolean found = false;
      for (int i=0; i<imageExtensions.length; i++)
      {   found = false;
          for (int j=0; j<imageImports.length; j++)
          {
             if (imageImports[j].equals(imageExtensions[i])) 
             {  found = true;  break;  }
          }
          if (found) extensions.add(imageExtensions[i]);
      }
      
      for (index=0; index < extensions.size(); index++)
      {  button = new JRadioButton(extensions.get(index), index==0);
         
         if (button != null)
         {
            button.addActionListener(
               new ActionListener()
               {  int selection = index;
            
                  public void actionPerformed(ActionEvent event)
                  {   imageSelect = selection;   }
               });
            imageButtons.add(button);
            imageGroup.add(button);
         }
      }
     
      JPanel dialog = new JPanel();
      dialog.add(soundButtons);
      dialog.add(imageButtons);
      
      // Create file chooser window.
      int option = AcornsProperties.SAVE;

      String[] exportArray = {"xml"};
      DialogFilter dialogFilter
      		= new DialogFilter("Export Files", exportArray);
      
      // Get the default name to use for export
      String[] properties = active.getProperties();
      properties[FileObject.NAME] = active.getDefaultName();

      String   separator  = System.getProperty("file.separator");
      String   selectName = properties[FileObject.PATH] + separator 
                 									              + properties[FileObject.NAME];
	  int lastIndex = selectName.lastIndexOf(".lnx");
	  if (lastIndex>=0) selectName = selectName.substring(0,lastIndex) + ".xml";
      if (!selectName.endsWith(".xml")) selectName += ".xml";

	  file = AppEnv.chooseFile(msgs[3], option, dialogFilter, selectName, dialog);

	   try
	   {
         if (file!=null)
         {
             // Add the .xml suffix to the file name if needed.
             fullName = file.getCanonicalPath();
             
             if (!SoundDefaults.isValidForSandbox(fullName))
             {
            	 return LanguageText.getMessage("acornsApplication", 161);
             }
             
             if (!fullName.endsWith(".xml"))
             {
                fullName = fullName + ".xml";
                file     = new File(fullName);
             }
            
             // Verify that the file can be read.
             if (Files.isFileOpen(fullName)) 
             { return LanguageText.getMessage
                                       ("acornsApplication", 68, fullName); }
         
             // Verify that it is okay to replace an existing file.
             if (file.exists()) 
             {
            	Frame frame = Environment.getRootFrame();
                int answer = JOptionPane.showConfirmDialog(frame, 
                        LanguageText.getMessage
                                       ("acornsApplication", 87, fullName)
                      , msgs[4], JOptionPane.OK_OPTION);

                 if (answer != JOptionPane.OK_OPTION) 
                      return LanguageText.getMessage("acornsApplication", 70);
             }      
             if (file.exists())
				         {   if (!file.delete())
                     return LanguageText.getMessage
                                        ("acornsApplication", 72, fullName); }
				 
             String directoryName = fullName.substring(0, fullName.length()-4);
             directoryF = new File(directoryName);
             if (directoryF.exists() && !deleteAll(directoryF))
				         {   return LanguageText.getMessage
                              ("acornsApplication", 72, directoryName);
				         }
				         directoryF.mkdir();
				 				 
         }   // End if if APPROVE_OPTION.
         else  return LanguageText.getMessage("acornsApplication", 70);
      
         // Get the path to the file and the file name.
         String name = file.getName();
         lastIndex   = fullName.lastIndexOf(name);
         if (lastIndex<0) return LanguageText.getMessage
                                        ("acornsApplication", 69, fullName);
         String path = fullName.substring(0,lastIndex-1);
         AppEnv.setPath(AcornsProperties.SAVE, path);
			
			String[] options = new String[2];
			options[AcornsProperties.SOUND_TYPE] = soundExtensions[soundSelect];
			options[AcornsProperties.IMAGE_TYPE] = extensions.get(imageSelect);
		   if (active.exportFile(file, directoryF, options)==null)
			{
			    return msgs[5];
			}
		}
      catch (IOException iox) 
      {
         return iox.toString();
      }
		return LanguageText.getMessage("acornsApplication", 104, fullName);
   }  // End of processOption().
	
	//----------------------------------------------------------
	// Method to delete all of the files in the sub-directory.
	//----------------------------------------------------------
	public boolean deleteAll(File directoryName)
	{
  	   File   file;
	   String separator = System.getProperty("file.separator");
		 
	   if (directoryName.isDirectory())
	   {
	       String[] directoryList = directoryName.list();
			  
	       for (int i=0; i<directoryList.length; i++)
	       {
		       file = new File(directoryName + separator + directoryList[i]);
		       if (!deleteAll(file)) return false;
		    }
		} 
      return directoryName.delete();
	}
}     // End of FileExport.
