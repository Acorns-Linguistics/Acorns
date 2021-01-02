/*
 * ToolsOptions.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.*;

import javax.swing.*;

import org.acorns.*;
import org.acorns.language.*;
import org.acorns.lesson.*;

public class ToolsOptions extends MenuOption
{
   public final static Dimension COMBOSIZE = new Dimension(200,15);
	
   public String processOption(String[] args)
   {
	  return preferences();
   }
   
   public static String preferences()
   {
	      boolean[] options = Environment.getOptions();
	      String[] msgs = LanguageText.getMessageList("acornsApplication", 122);
	      
	      Environment.getEnvironment();
	      String[] layerNames = Environment.getDefaultLayerNames();
			
	      // Create groups of radio buttons
	      // Sound on and off.
	      JLabel soundLabel = new JLabel(msgs[0]);
	      JRadioButton soundOn  
	                  = new JRadioButton(msgs[1], options[AcornsProperties.SPEECH]);
	      JRadioButton soundOff 
	                 = new JRadioButton(msgs[2], !options[AcornsProperties.SPEECH]);
	      ButtonGroup  soundGroup = new ButtonGroup();
	      soundGroup.add(soundOn);
	      soundGroup.add(soundOff);
			 
	      // Spelling on and off.
	     JLabel spellingLabel = new JLabel(msgs[3]);
	     JRadioButton spellingOn    
	             = new JRadioButton(msgs[1],  options[AcornsProperties.SPELLING]);
	     JRadioButton spellingOff   
	             = new JRadioButton(msgs[2],!options[AcornsProperties.SPELLING]);
	     ButtonGroup  spellingGroup = new ButtonGroup();
	     spellingGroup.add(spellingOn);
	     spellingGroup.add(spellingOff);

	      // Gloss on and off.
	      JLabel glossLabel = new JLabel(msgs[4]);
	      JRadioButton glossOn 
	              = new JRadioButton(msgs[1], options[AcornsProperties.GLOSS]);
	      JRadioButton glossOff 
	              = new JRadioButton(msgs[2], !options[AcornsProperties.GLOSS]);
	      ButtonGroup  glossGroup = new ButtonGroup();
	      glossGroup.add(glossOn);
	      glossGroup.add(glossOff);

	       // Create Panels to hold each set of options.
	      // Sound panel.
	      JPanel soundPanel = new JPanel();
	      soundPanel.setLayout(new BoxLayout(soundPanel, BoxLayout.Y_AXIS));
	      soundPanel.add(soundLabel);
	      soundPanel.add(soundOn);
	      soundPanel.add(soundOff);

	      // Spelling panel.
	      JPanel spellingPanel = new JPanel();
	      spellingPanel.setLayout(new BoxLayout(spellingPanel, BoxLayout.Y_AXIS));
	      spellingPanel.add(spellingLabel);
	      spellingPanel.add(spellingOn);
	      spellingPanel.add(spellingOff);

	      // Gloss panel.
	      JPanel glossPanel = new JPanel();
	      glossPanel.setLayout(new BoxLayout(glossPanel, BoxLayout.Y_AXIS));
	      glossPanel.add(glossLabel);
	      glossPanel.add(glossOn);
	      glossPanel.add(glossOff);
	      
	      // Panel for other options.
	      JPanel otherPanel = new JPanel();
	      otherPanel.setLayout(new GridLayout(3,1));
	      otherPanel.add(new JLabel(msgs[5]));
	      
	      //Drop down menu for keyboards.
	      JComboBox<String> combo = KeyboardFonts.getLanguageFonts().createLanguageComboBox(null, false);
	      combo.setToolTipText(msgs[6]);
	      combo.setPreferredSize(COMBOSIZE);
	      combo.setSize(COMBOSIZE);
	      combo.setMaximumSize(COMBOSIZE);
	      combo.setMinimumSize(COMBOSIZE);
	      otherPanel.add(combo);

	      // Create North Panel to hold each sub panel.
	      JPanel topPanel = new JPanel();
	      topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
	      topPanel.add(Box.createHorizontalGlue());
	      topPanel.add(soundPanel);
	      topPanel.add(Box.createHorizontalStrut(20));
	      topPanel.add(spellingPanel);
	      topPanel.add(Box.createHorizontalStrut(20));
	      topPanel.add(glossPanel);
	      topPanel.add(Box.createHorizontalStrut(20));
	      topPanel.add(otherPanel);
	      topPanel.add(Box.createHorizontalGlue());
	      
	      JPanel layerNamePanel = new JPanel();
	      layerNamePanel.setLayout(new GridLayout(2,layerNames.length/2));
	      JTextField[] textFields = new JTextField[layerNames.length];
	      for(int i=0; i<layerNames.length; i++)
	      {
	          textFields[i] = new JTextField();
	          textFields[i].setPreferredSize(new Dimension(175, 20));
	          textFields[i].setText(layerNames[i]);
	          layerNamePanel.add(textFields[i]);
	      }
	      
	      JPanel masterPanel = new JPanel();
	      masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.Y_AXIS));
	      topPanel.setAlignmentX(0);
	      masterPanel.add(topPanel);
	      masterPanel.add(Box.createVerticalStrut(10));
	      JLabel label = new JLabel(msgs[8]);
	      label.setAlignmentX(0);
	      masterPanel.add(label);
	      layerNamePanel.setAlignmentX(0);
	      masterPanel.add(layerNamePanel);
					
	      String title = msgs[7];
	      Frame frame = Environment.getRootFrame();
	      int result = JOptionPane.showConfirmDialog(frame, masterPanel, title
	                   , JOptionPane.CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

	      if (result == JOptionPane.OK_OPTION)			
	      {		
	          boolean[] newOptions = new boolean[AcornsProperties.MAX_OPTIONS];
	          newOptions[AcornsProperties.SPEECH] 
	                  = (soundOff.getSelectedObjects()==null);
	          newOptions[AcornsProperties.SPELLING] 
	                  = (spellingOff.getSelectedObjects()==null);
	          newOptions[AcornsProperties.GLOSS]  
	                  = (glossOff.getSelectedObjects()==null);
	          Environment.setOptions(newOptions);							

	          for (int i=0; i<layerNames.length; i++)
	          {
	              layerNames[i] = textFields[i].getText();
	          }

	          String language = (String)combo.getSelectedItem();
	          boolean ok = KeyboardFonts.getLanguageFonts().setLanguage(language);
	          if (ok==false)
	               return LanguageText.getMessage("acornsApplication",123,language);
	          else return LanguageText.getMessage("acornsApplication", 124);
	      }
	      else	{return LanguageText.getMessage("acornsApplication", 70);}
   }
}     // End of ToolsOptions class