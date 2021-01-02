/*
 * HelpAbout.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.*;
import javax.swing.*;
import org.acorns.language.*;
import org.acorns.lesson.AcornsProperties;
import org.acorns.*;

public class HelpAbout extends MenuOption
{
   public final static int ICON_SIZE = 50;
	
   public String processOption(String[] args)
   {
	  aboutMessage();
      return "ACORNS - [A][C]quisition [O]f [R]estored [N]ative [S]peech";
   }
   
   public static void aboutMessage()
   {
      // Create icon for the dialog window.
      ImageIcon image  = Environment.getIcon(AcornsProperties.ACORN , ICON_SIZE);
      String[] msgs = LanguageText.getMessageList("acornsApplication", 110);

      // Create label to hold the text.
      String[] text =
          { "ACORNS - [A][C]quisition Of [R]estored [N]ative [S]peech",
            "   ", msgs[0] + " " + AppObject.getVersion(),
            msgs[1]+" \u00a9 2019, Dan Harvey. "+msgs[2],
            "   ", msgs[3], "    harveyd@sou.edu",
            "    http://cs.sou.edu/cs/~harveyd/acorns",
            "   ",
            LanguageText.getMessage("acornsApplication",111),
            LanguageText.getMessage("acornsApplication",112),
            LanguageText.getMessage("acornsApplication",113),
            LanguageText.getMessage("acornsApplication",114),
            LanguageText.getMessage("acornsApplication",115)
       };

      JLabel[] labels = new JLabel[text.length];
      for (int i=0; i<text.length; i++)	{labels[i] = new JLabel(text[i]);}

      String title = msgs[4];
      Frame root = Environment.getFrame();
      JOptionPane.showMessageDialog
              (root, labels, title, JOptionPane.INFORMATION_MESSAGE, image);
   }
}
