/*
 * FileProperties.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved

 */
package acornsapplication;

import javax.swing.*;
import java.awt.*;
import java.beans.*;
import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.language.*;

public class FileProperties extends MenuOption
{
   public String processOption(String[] args)
   {
      return createPropertiesDialogue();
   }
	
  // Method to create a custom dialog box.
  private String createPropertiesDialogue()
  {
    FileObject active = Files.getActiveFile();

    // Get the name of the active file.
    final String[] properties = active.getProperties();
    String path = properties[FileObject.PATH];
    String name = properties[FileObject.NAME];
    String separator = System.getProperty("file.separator");
    String title  
       = LanguageText.getMessage("acornsApplication", 100, name);

    // Create components for the dialog window.
    final JLabel jLangLabel 
             = new JLabel(LanguageText.getMessage("acornsApplication", 101));
    jLangLabel.setHorizontalAlignment(JLabel.LEFT);
		
    final JTextField language = new JTextField("", 20);
    language.setText(properties[FileObject.LANG]);
    language.setAlignmentX(Component.LEFT_ALIGNMENT);
    language.setToolTipText(LanguageText.getMessage("acornsApplication", 94));
		
    final JLabel jAuthorLabel 
             = new JLabel(LanguageText.getMessage("acornsApplication", 95));
    jAuthorLabel.setHorizontalAlignment(JLabel.LEFT);
		
    final JTextField author = new JTextField("", 20);
    author.setText(properties[FileObject.AUTHOR]);
    author.setAlignmentX(Component.LEFT_ALIGNMENT);
    author.setToolTipText(LanguageText.getMessage("acornsApplication", 96));

    final JLabel jDescLabel 
              = new JLabel(LanguageText.getMessage("acornsApplication", 97));
    jDescLabel.setHorizontalAlignment(JLabel.LEFT);

    final JTextArea description = new JTextArea("", 5, 20);
    description.setText(properties[FileObject.DESC]);
    description.setLineWrap(true);
    description.setAlignmentX(Component.LEFT_ALIGNMENT);
    description.setToolTipText
              (LanguageText.getMessage("acornsApplication", 98));
		
    // Create panel to hold te components.
    JPanel panel = new JPanel();
    BoxLayout box = new BoxLayout(panel, BoxLayout.Y_AXIS);
    panel.setLayout(box);
		
    panel.add(jLangLabel);
    panel.add(language);
    panel.add(jAuthorLabel);
    panel.add(author);
    panel.add(jDescLabel);
    panel.add(description);
    panel.add(Box.createVerticalGlue());
		
    ImageIcon icon = Environment.getIcon(AcornsProperties.ACORN, 20);
    final JOptionPane optionPane = new JOptionPane
               (panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.CANCEL_OPTION);

    JFrame  frame  = new JFrame();
    frame.setIconImage(icon.getImage());							
    final JDialog dialog = new JDialog(frame, title, true);
    dialog.setContentPane(optionPane);
		
    optionPane.addPropertyChangeListener
    (   new PropertyChangeListener()
        {  public void propertyChange(PropertyChangeEvent event)
           {   String prop = event.getPropertyName();
				 
               if (dialog.isVisible() && (event.getSource() == optionPane)
		                   && (prop.equals(JOptionPane.VALUE_PROPERTY)))
               {
                  dialog.dispose();
               }
           }
        }
    );
		
    dialog.pack();
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);

    try
    {
       Integer value = (Integer)optionPane.getValue();
       if (value.intValue() == JOptionPane.YES_OPTION)
       {
          active.setProperties(description.getText(), language.getText()
                                                    , author.getText());
          return LanguageText.getMessage
                    ("acornsApplication", 99, path + separator + name);
       }
    }
    catch (ClassCastException e) {}

    return LanguageText.getMessage("acornsApplication", 70);
  }
}
