/**
 * ToolsFeedbackRecordings.java
 * @author HarveyD
 * @version 4.00 Beta
 *
 * Copyright 2007-2015, all rights reserved
 */

package acornsapplication;

import javax.swing.*;

import java.awt.Frame;
import java.awt.event.*;
import java.util.*;
import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.data.*;
import org.acorns.language.*;

/** This class enables users to retrieve feedback sounds for
 *     good answer, close but not quote correct, wrong answer.
 */
public class ToolsFeedbackRecordings extends MenuOption
                                           implements ActionListener
{
   private static JButton[] buttons;
   public  static JPanel masterPanel;

   private static PicturesSoundData[] feedback;

   /*
    * This is a polymorphic method that processes user commands
    *
    * @param status The Object controlling the Acorns environment
    * @param args An array of user arguments needed to process this command
    */
   public String processOption(String[] args)
   {   feedback = Environment.getFeedback();

       if (masterPanel==null)
       {
           masterPanel = new JPanel();
           masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.X_AXIS));

           String[] names 
                        = LanguageText.getMessageList("acornsApplication", 126);
           buttons = new JButton[3];

           masterPanel.add(Box.createHorizontalGlue());
           for (int i=0; i<names.length; i++)
           {
               buttons[i] = new JButton(names[i]);
               buttons[i].addActionListener(this);
               masterPanel.add(Box.createHorizontalStrut(25));
               masterPanel.add(buttons[i]);
           }
           masterPanel.add(Box.createHorizontalStrut(25));
           masterPanel.add(Box.createHorizontalGlue());
       }

       String title = LanguageText.getMessage("acornsApplication", 127);

       int result = JOptionPane.showConfirmDialog
               (Environment.getFrame(), masterPanel, title
		        , JOptionPane.CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

      if (result == JOptionPane.OK_OPTION)
      {   boolean hasRecording = true;
          for (int i=0; i<3; i++)
          {
             Vector<SoundData> vector = feedback[i].getVector();
             for (int j=0; j<vector.size(); j++)
             {   if (!vector.get(j).isRecorded())
                 {   vector.remove(j);
                     hasRecording = false;
                 }
             }
          }
          if (!hasRecording)
          {
              JOptionPane.showMessageDialog
                      (masterPanel, LanguageText.getMessage("acornsApplication", 128));
              return LanguageText.getMessage("acornsApplication", 129);
          }
          Environment.saveFeedback(feedback);
	  return LanguageText.getMessage("acornsApplication", 46);
      }
      else return LanguageText.getMessage("acornsApplication", 45);

   } // End of processOption method.

   public void actionPerformed(ActionEvent event)
   {
       JButton button = (JButton)event.getSource();

       try
       {  Lesson lesson = LessonTypes.getBlankLesson();

          if (button == buttons[AcornsProperties.CORRECT])
          {
             feedback[AcornsProperties.CORRECT].pictureDialog(lesson, masterPanel);
          }
          if (button == buttons[AcornsProperties.SPELL])
          {
             feedback[AcornsProperties.SPELL].pictureDialog(lesson, masterPanel);
          }
          if (button == buttons[AcornsProperties.INCORRECT])
          {
             feedback[AcornsProperties.INCORRECT].pictureDialog(lesson, masterPanel);
          }
       }  catch (Exception e)
       {
    	   Frame frame = Environment.getRootFrame();
           JOptionPane.showMessageDialog(frame, e.toString());
       }
   }
}    // End of ToolsFeedbackRecordings class.
