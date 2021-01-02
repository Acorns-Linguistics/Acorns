 /* FileMakeMobileApp.java
 *
 * Created on September 22, 2011, 3:07 PM
 *
 *
 *   @author  HarveyD
 *   @version 7.00 Beta
 *
 *   Copyright 2011-2015, all rights reserved
 */
package acornsapplication;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.acorns.Environment;
import org.acorns.FileObject;
import org.acorns.audio.SoundDefaults;
import org.acorns.data.SoundData;
import org.acorns.language.FontHandler;
import org.acorns.language.KeyboardFonts;
import org.acorns.language.LanguageText;
import org.acorns.lesson.AcornsProperties;
import org.acorns.lib.DialogFilter;

import com.google.typography.font.sfntly.FontFactory;
import com.google.typography.font.sfntly.data.WritableFontData;
import com.google.typography.font.tools.conversion.woff.WoffWriter;

//----------------------------------------------------------
// Class to export a file with a selected name.
//----------------------------------------------------------
public class FileMakeMobileApp extends MenuOption
{
	private static Thread thisThread;
	
	private final String[] icons 
       = {"acorn.png", "anchor.png", "answers.png", "begin.png"
        , "check.png", "down.png", "end.png", "help.png", "info.png"
        , "moveleft.png", "moveright.png", "next.png", "pause.png"
        , "play.png", "prev.png", "random.png", "replay.png"
        , "record.png", "stop.png", "up.png", "slowdown.png" };
	
	private final String[] audios
	  = {"correct.mp3",
		 "incorrect.mp3",
		 "beep.mp3",
		 "spell.mp3",
		 "silence.mp3" };
	
	private int[] feedbackVectors = 
		    { AcornsProperties.CORRECT
			, AcornsProperties.INCORRECT
			, AcornsProperties.SPELL };

	private String[] feedbackNames = { "correct", "incorrect", "spell", "slow" };
	
	private String separator = System.getProperty("file.separator");
	   
	//----------------------------------------------------------
	// Method to process export options.
	//----------------------------------------------------------
	public String processOption(String[] args)
	{
	   if (thisThread!=null && thisThread.isAlive()) 
	   {
		   Toolkit.getDefaultToolkit().beep();
		   return LanguageText.getMessage("acornsApplication", 70);
	   }
	   
	   File         file = null; // File object for selected export file
	   File         directoryF;  // File directory object for lesson
	   String       fullName;    // Name of selected export file name.

	   String[] msgs = LanguageText.getMessageList("acornsApplication", 102);
	   String[] errors = LanguageText.getMessageList("acornsApplication", 159);
			
	   // Get active file.
	   AppObject active = Files.getActiveFile();
			
	   // Create panel allowing users to customize the page
	   String[] optionText = LanguageText.getMessageList("acornsApplication", 106);
	   JLabel titleLabel = new JLabel(optionText[1]);
	   titleLabel.setAlignmentX(0);

	   JTextField title = new JTextField();
	   title.setText(
	     active.getLessonInfo().getPanelInfo()[AcornsProperties.TITLE]);
	   title.setPreferredSize(new Dimension(150,20));
	   title.setMinimumSize(new Dimension(150,20));
	   title.setMaximumSize(new Dimension(150,20));
	   title.setAlignmentX(0);
	   
	   JCheckBox recordEnabled = new JCheckBox();
	   recordEnabled.setText(optionText[7]);
	   	   
	   JPanel dialog = new JPanel();
	   dialog.setLayout(new BoxLayout(dialog, BoxLayout.Y_AXIS));
	   dialog.add(titleLabel);
	   dialog.add(title);
	   dialog.add(Box.createVerticalStrut(20));
	   dialog.add(recordEnabled);
	      
	   // Create file chooser window.
	   int option = AcornsProperties.SAVE;
	   String[] properties = active.getProperties();
	   properties[FileObject.NAME] = active.getDefaultName();
   
	   String   selectName 
	         = properties[FileObject.PATH] + separator + properties[FileObject.NAME];
	   int lastIndex = selectName.lastIndexOf(".lnx");
	   if (lastIndex>=0) selectName = selectName.substring(0,lastIndex) + ".html";
	   if (!selectName.endsWith(".html")) selectName += ".html";

	   final String[] exportArray = {"html"};
	   final DialogFilter dialogFilter
	                 = new DialogFilter("Web Page Files", exportArray);

	   file = AppEnv.chooseFile(msgs[3], option, dialogFilter, selectName, dialog);
			
	   try
	   {
	      if (file == null)
	    	  return LanguageText.getMessage("acornsApplication", 70);

	      // Approve option - begin to create mobile application
	      // Add the .html suffix to the file name if needed.
          fullName = file.getCanonicalPath();
          
          if (!SoundDefaults.isValidForSandbox(fullName))
         	 return LanguageText.getMessage("acornsApplication", 161);
		  
          if (!fullName.endsWith(".html"))
          {
        	 lastIndex = fullName.lastIndexOf(".");
        	 if (lastIndex>=0) fullName = fullName.substring(0,lastIndex) + ".html";
        	 else  fullName = fullName + ".html";
             file     = new File(fullName);
          }
            
          // Verify that the file can be read.
          if (Files.isFileOpen(fullName)) 
          { 
        	  return LanguageText.getMessage
                                    ("acornsApplication", 68, fullName); 
          }
         
          // Verify that it is ok to replace an existing file.
          if (file.exists()) 
          {
    	      Frame frame = Environment.getRootFrame();
              int answer = JOptionPane.showConfirmDialog(frame, 
                  LanguageText.getMessage("acornsApplication", 87, fullName),
                  msgs[4], JOptionPane.OK_OPTION);

              if (answer != JOptionPane.OK_OPTION) 
                  return LanguageText.getMessage("acornsApplication", 70);
          }
             
          if (file.exists())
		  {   
           	 if (!file.delete())
                  return LanguageText.getMessage
                           ("acornsApplication", 72, fullName); 
          }
				 
          String directoryName = fullName.substring(0, fullName.length()-5);
          directoryF = new File(directoryName);
          if (directoryF.exists() && !deleteAll(directoryF))
		  {   
             return LanguageText.getMessage
                           ("acornsApplication", 72, directoryName);
	 	  }
		  directoryF.mkdir();
		  
		  // Make the sub-folder directories containing resources
		  File assetF = new File(directoryName, "Assets");
		  assetF.mkdir();
		  if (!copyMobileFiles(assetF, recordEnabled.isSelected())) { return errors[3]; }
		  
		  // Copy the icons needed for the mobile application
		  File iconF = new File(assetF, "Icons");
		  iconF.mkdir();
		  if (!copyIcons(iconF))	  {  return errors[0];  }
		  
		  // Copy the embedded fonts needed for the mobile application
		  File fontF = new File(assetF, "Fonts");
		  fontF.mkdir();
		  if (!copyFonts(fontF)) return errors[1];
		  
		  // Copy the audio files needed for the mobile application
		  File  audioF = new File(assetF, "Audio");
		  audioF.mkdir();
		  if (!copyAudio(audioF))  { return errors[2]; }
		  
	      String name = file.getName();
		  String shortName = name.substring(0,name.length()-5);
		  Boolean selected = recordEnabled.isSelected();
		  
	      // Get the path to the file and the file name.
	      lastIndex   = fullName.lastIndexOf(name);
	      if (lastIndex<0) return LanguageText.getMessage
	                                     ("acornsApplication", 69, fullName);
	      String path = fullName.substring(0,lastIndex-1);
	      AppEnv.setPath(AcornsProperties.SAVE, path);
	      
	      thisThread 
	           = new DoExport(directoryF, title.getText()
	        		   , fullName, shortName, selected);
	      
		}
	    catch (IOException iox) 
	    {
	       return iox.toString();
	    }
		return LanguageText.getMessage("acornsApplication", 109);
	}  // End of processOption().
		
	/** Method to delete all of the files in the sub-directory.
	 * 
	 * @param directoryName File object containing directory
	 * @return true if delete successful
	 */
	private boolean deleteAll(File directoryName)
	{
	   File   file;
			 
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
	
	/** Copy a file from a URL address to an output destination
	 * 
	 * @param url The URL for the source file
	 * @param output The destination file object
	 * @return true if successful
	 */
	private boolean copyURLToFile(URL url, File output)
	{
	     BufferedInputStream inStream = null;
	     BufferedOutputStream outStream = null;
	     
	     try 
	     {
	        int bufSize = 8192;
	        inStream = new BufferedInputStream
	    	  	   			( url.openConnection().getInputStream(), bufSize);
	       
	        outStream = new BufferedOutputStream(new FileOutputStream(output), bufSize);
	          
            int read = -1;
	        byte[] buf = new byte[bufSize];
	        while ((read = inStream.read(buf, 0, bufSize)) >= 0) 
	        {
	           outStream.write(buf, 0, read);
	        }
	        outStream.flush();	      
	     }
	     catch (Exception ex)  { return false; }
	     finally 
	     {
            try { inStream.close(); }  catch (Exception ex) {}
            try { outStream.close(); } catch (Exception cioex) {}
	     }
	     return true;
	}
	
	/** Copy the list of files to an output directory
	 * 
	 * @param files The list of file names
	 * @param outputDir The destination directory
	 * @return true if successful
	 */
	private boolean copyFileList(String[] files, File outputDir)
	{
		String fileName;
		for (int i=0; i<files.length; i++)
		{
			fileName = "/data/" + files[i];
            URL url = getClass().getResource(fileName);
            if (!copyURLToFile(url, new File(outputDir, files[i]))) return false;
		}
		return true;
	}
	
	/** Copy icons needed for the web application
	 * 
	 * @param outputDir The path to the output directory
	 * @return true if successful
	 */
	private boolean copyIcons(File outputDir)
	{
		return copyFileList(icons, outputDir);
	}
	
	/** Copy embedded fonts needed for the web application
	 * 
	 * @param outputDir The path to the output directory
	 * @return true if successful
	 */
	private boolean copyFonts(File outputDir)
	{
		KeyboardFonts keyboardFonts = KeyboardFonts.getLanguageFonts();
		
		String[] languages = keyboardFonts.getLanguages();
		File destination, source;
		Font font;
		String fileName;
		
		try
		{
			for (int f=0; f<languages.length; f++)
			{
			    font = keyboardFonts.getFont(languages[f]);
				source = FontHandler.getHandler().getFontPath(font);
				if (source==null) continue;
				
				fileName = font.getName().replaceAll("\\s+", "") + ".ttf";
			    destination = new File(outputDir, fileName);
			    writeTTF(source, destination);
		
				fileName = fileName.substring(0,fileName.lastIndexOf("."))+".woff";
				destination = new File(outputDir, fileName);
			    writeWoff(source, destination);
			}
		}
		catch (Exception e) 
		{ 
			return false; 
		}
		return true;
	}
	
	/** Copy the feedback audio files needed for the web application 
	 * 
	 * @param outputDir The path to the output directory
	 * @return true if successful
	 */
	private boolean copyAudio(File outputDir)
	{
		if (!copyFileList(audios,outputDir)) return false;
		
		SoundData sound;
		Vector<SoundData> soundVector;
		for (int i=0; i<feedbackVectors.length; i++)
		{
			soundVector 
			    = Environment.getFeedbackVector(feedbackVectors[i]);
			if (soundVector==null) continue;
			
			try
			{
				for (int j=0; j<soundVector.size(); j++)
				{
					sound = soundVector.get(j);
					sound.writeFile(outputDir + separator
					           + feedbackNames[i] + j + ".mp3");
				}
			}
			catch (Exception e) { return false; }	
		}
		return true;
	}
	
	/** Copy the executables needed for the mobile application
	 * 
	 * @param outputDir The path to the output directory
	 * @return true if successful
	 */
	private boolean copyMobileFiles(File outputDir, boolean selected)
	{
		String[] files = {"acorns.js"};
		if (selected)
		{
			files = new String[]{"acorns.js", "DesktopAcornsAudioExtension.jar"};
		}
		
		File outputFile;
		URL url;
		
		for (int i=0; i<files.length; i++)
		{
			try 
			{
				String fileName = "/data/" + files[i];
				url = getClass().getResource(fileName);
			}
			catch (Exception e) { return false; }
			
			outputFile = new File(outputDir, files[i]);
	        if (!copyURLToFile(url, outputFile)) return false;
		}
		return true;
	}
	
	/** Copy the HTML file template to the destination 
	 * 
	 * @param sink The destination filename
	 * @return true if successful
	 */
	private boolean makeHTMLFile(String sink, String shortName
			       , String title, String recordEnabled, String xmlString)
	{
		String fileName = "/data/" + "mobileTemplate.html";
		URL url = getClass().getResource(fileName);

		String fonts = createFontFace(shortName);
        xmlString = xmlString.replaceAll("\"", "\\\\\""); 
        xmlString = xmlString.replaceAll("(\\r|\\n)+","\\\\n");

		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			BufferedWriter bufWriter = new BufferedWriter
				    (new OutputStreamWriter(new FileOutputStream(sink),"UTF-8"));
			PrintWriter out = new PrintWriter(bufWriter);
			
			String line = in.readLine();
			while (line!=null)
			{
				if (line.indexOf("FONTS")>=0)
					out.println(fonts);
				else if (line.indexOf("LANGUAGES")>=0)
				{
					out.println(createLanguageList());
					
				}
				else if (line.indexOf("FEEDBACK")>=0)
				{
					out.println(createFeedbackList());
				}
				else if (line.indexOf("XML")>=0)
				{
					out.print("\t\tvar xml =\"");
			        xmlString = xmlString.replaceAll("(\\n[ ])+","");
					out.print(xmlString);
					out.println("\";");
					
					String xmlFont = createEmbeddedList();
					
					out.println(xmlFont);

					
				}
				else
				{
					line = line.replaceAll("HHHH", shortName);
					line = line.replaceAll("TTTT", title);
					line = line.replaceAll("RECORD", recordEnabled);
					out.println(line);
				}
				line = in.readLine();
			}
			in.close();
			out.close();
		}
		catch (Exception e) { return false; }
		return true;
	}
	
	/** Copy embedded fonts needed for the web application
	 * 
	 * @return String of HTML font-face tags
	 */
	private String createFontFace(String shortName)
	{
		Font font;
		String family, fontName;
		StringBuilder buffer = new StringBuilder();
		
		KeyboardFonts keyboardFonts = KeyboardFonts.getLanguageFonts();
		String[] fontList = keyboardFonts.getLanguages();

        for (int i=0; i<fontList.length; i++)
        {
    	   try 
    	   {
 			   font = keyboardFonts.getFont(fontList[i]);
    	       family = font.getFamily();
			   fontName = font.getName().replaceAll("\\s+", "");

	       	   buffer.append("\t\t@font-face  { 	font-family:\"");
	       	   buffer.append(family);
	       	   buffer.append("\";\n\t\t\t\t\t\t");
	
	       	   buffer.append("src: ");
	       	   buffer.append("local(\"" + family + "\"),");
	       	   buffer.append("\n\t\t\t\t\t\t\t");
	       	   
	       	   buffer.append(" url(\"" + shortName);
	       	   buffer.append("/Assets/Fonts/");
	       	   buffer.append(fontName);
	       	   buffer.append(".woff\") format(\"woff\"),\n\t\t\t\t\t\t\t");
	
	       	   buffer.append(" url(\"");
	       	   buffer.append(shortName);
	       	   buffer.append("/Assets/Fonts/");
	       	   buffer.append(fontName);
	       	   buffer.append(".ttf\") format(\"truetype\");\n\t\t\t\t\t}\n\n");
    	   } 
    	   catch (Exception ex) {}
		}
		return buffer.toString();
	}
	
	/** Create list of languages needed for the mobile application */
	private String createLanguageList()
	{
		StringBuilder buffer = new StringBuilder();

		KeyboardFonts keyboardFonts = KeyboardFonts.getLanguageFonts();
		String[] languages = keyboardFonts.getLanguages();
		Font font;
		
		for (int i=0; i<languages.length; i++)
		{
			font = keyboardFonts.getFont(languages[i]);
			buffer.append("\t\t\t\"");
			buffer.append(languages[i]);
			buffer.append("\": { \"Family\": \"");
			buffer.append(font.getName());
			buffer.append("\", \"Size\": ");
			buffer.append(font.getSize());
			buffer.append(" }");
			if (i<languages.length-1) buffer.append(",");
			buffer.append("\n");
		}
		return buffer.toString();		
	}
	
	/** Create an xml string of keylayout data */
	private String createEmbeddedList()
	{
		StringBuilder buffer = new StringBuilder();

		KeyboardFonts keyboardFonts = KeyboardFonts.getLanguageFonts();
		ArrayList<String[]> fonts = keyboardFonts.exportEmbeddedFonts();
		for (String[] xml: fonts)
		{
			buffer.append("\t\t" + xml[1] + "\n");
		}

		return buffer.toString();		
	}
	
	private String createFeedbackList()
	{
		StringBuilder buffer = new StringBuilder();

		Vector<SoundData> soundVector;

		for (int i=0; i<feedbackNames.length; i++)
		{
			if (i < feedbackVectors.length)
			{
				soundVector 
				      = Environment.getFeedbackVector(feedbackVectors[i]);
				if (soundVector==null) continue;
			}
			else  soundVector = new Vector<>();
			
			buffer.append("\t\t\t\"");
			buffer.append(feedbackNames[i]);
			buffer.append("\": [");

			if (soundVector.isEmpty())
			{
				if (i < feedbackVectors.length)
				{
					buffer.append("\"");
					buffer.append(feedbackNames[i]);
					buffer.append(".mp3\"");
				}
				else buffer.append("\"\"");
			}
			else for (int j=0; j<soundVector.size(); j++)
			{
				buffer.append("\"");
				buffer.append(feedbackNames[i]);
				buffer.append(j);
				buffer.append(".mp3\"");
				if (j<soundVector.size()-1) buffer.append(", ");
			}
			if (i<feedbackNames.length-1) buffer.append(" ],\n");
			else buffer.append(" ]\n");
		}
		return buffer.toString();
	}
	
	class DoExport extends Thread
	{
		File directoryF;
		String title, fullName, shortName;
		Boolean selected;
		
		public DoExport(File directoryF, String title
				, String fullName, String shortName, Boolean selected)
		{
			this.directoryF = directoryF;
			this.title = title;
			this.fullName = fullName;
			this.shortName = shortName;
			this.selected = selected;
			start();
		}
		
		public void run()
		{
			String[] options = new String[2];
			options[AcornsProperties.SOUND_TYPE] =  "mp3";
			options[AcornsProperties.IMAGE_TYPE] = "Default";
			
			AppObject active = Files.getActiveFile();
  		    String[] errors = LanguageText.getMessageList("acornsApplication", 159);
  		    String[] msgs = LanguageText.getMessageList("acornsApplication", 102);
			
  		    String recordText = selected.toString();
            String directoryName = fullName.substring(0, fullName.length()-5);
            
            Environment.setText( msgs[7]);
			String xmlString = active.exportFile(null, directoryF, options);
			if (!makeHTMLFile(fullName, shortName
			                  , title, recordText, xmlString))  
			{ 
				Environment.setText( errors[4]); return; 
			}
			
			String xmlName = directoryName + ".xml";
			Environment.setText( msgs[8]);
			if (active.exportFile(new File(xmlName), directoryF, options)==null)
			{ 
			    Environment.setText( msgs[5]); return; }  {
			}
			    
			try
			{
				new ZipWebPage(directoryName);
				Environment.setText( msgs[6]);
			}
			catch (Exception e)
			{
				Environment.setText(e.toString());
			}
		}
	}
	
	/** Write the font to disk
	 * 
	 * @param source The path to the font to write
	 * @param file The path for writing the font
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    private void writeTTF(File source, File file) 
			throws IOException, FileNotFoundException
	{
		FileInputStream input = new FileInputStream(source);
		byte[] buf = new byte[1024];
		int len;

		// Write to output file
		FileOutputStream output = new FileOutputStream(file);
		while ((len=input.read(buf)) > 0)  
		{ 
			output.write(buf, 0, len); 
		}
		input.close();
		output.close();
	}
    
    /** Create woff version of the font 
     * 
	 * @param font The font to write
	 * @param file The path for writing the font
     */
    private void writeWoff(File source, File file)
    {
        int len = 1024;
        byte[] bytes, buf = new byte[len];
        
        try 
        {
        	// Read file into byte array
 //   		File source = getFontPath(font);
    		FileInputStream input = new FileInputStream(source);
    		ByteArrayOutputStream stream = new ByteArrayOutputStream();
    		while ((len=input.read(buf)) > 0)  
    		{ 
    			stream.write(buf, 0, len); 
    		}
    		bytes = stream.toByteArray();
    		input.close();
 
    		// Convert to woff
            WoffWriter ww = new WoffWriter();
            FontFactory fontFactory = FontFactory.getInstance();

            com.google.typography.font.sfntly.Font woff = fontFactory.loadFonts(bytes)[0];
            WritableFontData wfd = ww.convert(woff);

        	FileOutputStream output = new FileOutputStream(file);
            wfd.copyTo(output);
            output.close();

        } catch (IOException e1) { e1.printStackTrace(); }
    }
	
}      // end of FileMakeMobileApp

