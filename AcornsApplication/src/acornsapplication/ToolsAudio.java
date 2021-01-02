/*
 * ToolsAudio.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.*;
import javax.swing.*;
import org.acorns.audio.*;
import org.acorns.editor.*;
import org.acorns.language.*;

public class ToolsAudio extends MenuOption
{
   public final static Dimension COMBOSIZE = new Dimension(200,15);
	
   public String processOption(String[] args)
   {
      // Create Sound Options panel.
      OptionPanel options = new OptionPanel();
      JPanel thisPanel = null;
      
      //options.setBackground(SoundDefaults.BACKGROUND);
      Frame frame = JOptionPane.getRootFrame();
      Component[] components = frame.getComponents();
      for (int c=0; c<components.length; c++)
      {  components[c].setBackground(SoundDefaults.BACKGROUND);
      }
      frame = JOptionPane.getFrameForComponent(thisPanel);
      components = frame.getComponents();
      for (int c=0; c<components.length; c++)
      {  components[c].setBackground(SoundDefaults.BACKGROUND);
      }
      components = options.getComponents();
      for (int c=0; c<components.length; c++)
      {  components[c].setBackground(SoundDefaults.BACKGROUND);
      }

      // Display dialog and get user response.           
      String[] dialogOptions 
              = LanguageText.getMessageList("acornsApplication", 131);
      String title 
          = LanguageText.getMessage("acornsApplication", 135);

      int result  = JOptionPane.showOptionDialog
         (thisPanel, options, title
         , JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE
         , null, dialogOptions, dialogOptions[1]);

      if (result == 0)  // Index into dialog option array.
      {  if (!options.updateValues())
         {  return LanguageText.getMessage("acornsApplication", 132);
        }
      }
      else return LanguageText.getMessage("acornsApplication", 133);
      
      return LanguageText.getMessage("acornsApplication", 134);
   }        // End of processOption();
}           // End of ToolsAudio class.