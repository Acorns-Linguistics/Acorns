/*
 * EditSoundEditor.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.*;
import javax.swing.*;
import org.acorns.*;
import org.acorns.editor.*;
import org.acorns.language.*;

public class EditSoundEditor extends MenuOption
{  SoundPanels soundPanels;
   
   // Method called polymorphically from AcornMenu.
   public String processOption(String[] args)
   { 
	  // Execute the sound editor
      Files.setMode(true);
      Environment.getFrame().setVisible(false);
      JFrame playFrame = (JFrame)Environment.getPlayFrame();
      Container container = playFrame.getContentPane();
      container.removeAll();
         
      // Create and add the sound panels.
      soundPanels = new SoundPanels(2);
      playFrame.add(soundPanels);
      playFrame.setVisible(true);
      playFrame.setLocationRelativeTo(null);
          
      playFrame.validate();
      playFrame.repaint();

      return LanguageText.getMessage("acornsApplication", 75);
   }
}
