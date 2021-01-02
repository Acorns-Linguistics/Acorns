/*
 * ToolsExecute.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.Point;

import javax.swing.*;
import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.language.*;

public class ToolsExecute extends MenuOption
{
   // Method called polymorphically from AcornMenu.
   public String processOption(String[] args)
   {
      FileObject active = Files.getActiveFile();
      Lesson lesson = active.getActiveLesson();
      if (lesson.isPlayable())
      {
         // Execute the active lesson.
         Files.setMode(true);
         active.displayLesson();
         JPanel lessonData = AppEnv.getDataPanel();
         lessonData.removeAll();

         JFrame frame = Environment.getFrame();
         Point point = frame.getLocation();
         JFrame playFrame = (JFrame)Environment.getPlayFrame();

         SwingUtilities.invokeLater( new Runnable() {
        	 public void run()
        	 {
                 playFrame.setVisible(true);
                 playFrame.setLocation(point);
                 frame.setVisible(false);
        	 }
         });
         return LanguageText.getMessage("acornsApplication", 130);
      }
      else return LanguageText.getMessage("acornsApplication", 32);
   }
}
