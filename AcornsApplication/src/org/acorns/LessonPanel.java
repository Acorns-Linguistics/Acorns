/*
 * LessonPanel.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package org.acorns;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

import org.acorns.lesson.*;
import org.acorns.language.*;

/** Class to hold a panel of lesson header information */
public class LessonPanel extends JPanel implements DocumentListener
{
   private final static long serialVersionUID = 1;

   private final static int  MAX_FIELD = 30, DRAG_COUNT = 2;

   // Variables to handle lesson panel repositioning.
   private int       drag = 0;
   private Point     mouseAt;
   private ImageIcon icon;
   private int       delta;	
   
   // Lesson panel components.
   private JLabel    typeLabel, titleLabel, descriptionLabel;
   private JTextField nameField;
   private LayerPanel[] layerNames;
   private JComboBox<String> comboBox = null;
   
   // Array of available lesson types and the type selected.
   private Lesson[] lessonObjects;
   private String[] types, languageTypes;
   private int      lessonType;
   
   /** Create panel using array of strings containing header information
    *
    * @param infoString Array of lesson header strings
    */
   public LessonPanel(String[] infoString) throws InvalidFileTypeException
   {
      // Create array of choices for lesson type.
      lessonObjects = LessonTypes.getAvailableLessonObjects();
      languageTypes = new String[lessonObjects.length+1];
      types = new String[lessonObjects.length+1];
		    
      languageTypes[0] = LanguageText.getMessage("acornsApplication", 13);
      for (int i=0; i<lessonObjects.length; i++)
      {
         languageTypes[i+1] = lessonObjects[i].getLanguageName();
         types[i+1] = lessonObjects[i].getName();
      }  // End of For loop.
		
      // Create panel components.
      typeLabel  = new JLabel();
      typeLabel.setFont(new Font(null, Font.PLAIN, 10));
      titleLabel = new JLabel();
      titleLabel.setFont(new Font(null, Font.PLAIN, 10));
      nameField = new JTextField(10);
      Dimension fieldSize = new Dimension(300, 20);
      nameField.setPreferredSize(fieldSize);
      nameField.setMaximumSize(fieldSize);
      nameField.setSize(fieldSize);
      nameField.setFont(new Font(null, Font.PLAIN, 10));
      descriptionLabel  = new JLabel();
      descriptionLabel.setFont(new Font(null, Font.PLAIN, 10)); 

      layerNames = new LayerPanel[AcornsProperties.MAX_LAYERS];
      for (int i=0; i<layerNames.length; i++)
      {   layerNames[i] = new LayerPanel( "" + (i+1), "", 10);  }
      
      if (infoString==null)   
      {    if (!dialog(infoString)) throw new InvalidFileTypeException();   }
      else setPanelInfo(infoString);
      
      for (int i=1; i<types.length; i++)
      {   if (languageTypes[i].equals(typeLabel.getText()))
          { lessonType=i; break; }
      }
      if (lessonType==0) throw new InvalidFileTypeException();
      
      // Set layout and add components.
      setLayout(new GridLayout(16,1));
      add(typeLabel);
      add(titleLabel);
      add(descriptionLabel);
      add(new JLabel(LanguageText.getMessage("acornsApplication", 15)));
      add(nameField);
      add(new JLabel(LanguageText.getMessage("acornsApplication", 16)));
      for (int i=0; i<layerNames.length; i++)    add(layerNames[i]);

      Dimension size = new Dimension(200,300);
      setPreferredSize(size);
      setMaximumSize(size);
      
  }   // End of LessonPanel constructor.
   
  // Find current version for a particular lesson type.
  public int findVersion()
  {
	  String lessonName = getPanelInfo()[0];
	  for (int i=1; i<types.length; i++)
	  {
		  if (lessonName.equals(types[i]))
		  { 
			  String className = lessonObjects[i-1].getClass().getCanonicalName();
			  className = className.substring("org.acorns.Lesson.".length());
			  className = className.substring(0, className.indexOf("."));
			  if (className.length()>0)
			  {
				  char c = className.charAt(className.length()-1);
				  if (Character.isDigit(c)) return Character.getNumericValue(c);
			  }
		  }
	  }
	  return FileObject.version.charAt(0);
  }

   /** Paint component during a lesson drag operation
    *
    * @param page Object to draw into during a drag operation
    */
  public @Override void paintComponent(Graphics page)
  {  
      super.paintComponent(page);
		
      Rectangle bounds = getBounds();
      if (drag>0)
      {
         Dimension size = getSize();
         page.drawLine(0, mouseAt.y-bounds.y, size.width, mouseAt.y-bounds.y);
         icon.paintIcon(this, page, mouseAt.x-delta, mouseAt.y-delta-bounds.y);
         drag--;
      }
    
  }  // End of paintComponent()

   
   /** Method to triger a redraw operation
    *
    * @param mouseAt position of the mouse during the redraw operation
    * @param icon to draw based on the position of the mouse
    * @param delta the number of pixels up and to the left of the mouse position
    * where icon should be drawn
    * @param dragOn true if a mouse drag operation is in progress
    */
   public void paintIt(Point mouseAt, ImageIcon icon, int delta, boolean dragOn)
   {
      this.mouseAt = mouseAt;
      this.icon    = icon;
      this.delta   = delta;
   
      if (dragOn)  drag = DRAG_COUNT;
      else         drag = 0;

      repaint();
   }    // End of paintIt()

   /** Return the object corresponding to this lesson type */
   public Lesson getLessonObject()  { return lessonObjects[lessonType-1];  }
   
   /** Get array of strings defining the lesson header */
  public String[] getPanelInfo()
  {
     String[] infoString = new String[AcornsProperties.TYPES];

     infoString[AcornsProperties.TYPE] = typeLabel.getText();
     for (int i=1; i<types.length; i++)
     {   if (languageTypes[i].equals(infoString[AcornsProperties.TYPE]))
         {   infoString[AcornsProperties.TYPE] = types[i]; break; }
     }
     infoString[AcornsProperties.TITLE] = titleLabel.getText();
     infoString[AcornsProperties.NAME]  = nameField.getText();
     infoString[AcornsProperties.DESC]  = descriptionLabel.getText();
		
     for (int i=0; i<layerNames.length; i++)
     {  infoString[AcornsProperties.LAYERNAMES + i] = layerNames[i].getText();
     }
     return infoString;
  }  // End of getPanelInfo.

   /** Modify the lesson header information
    *
    * @param infoString Array of strings defining the lesson header information
    */
   public final void setPanelInfo(String[] infoString)
   {
       nameField.getDocument().removeDocumentListener(this);

       typeLabel.setText(infoString[AcornsProperties.TYPE]);
       for (int i=1; i<types.length; i++)
           if (infoString[AcornsProperties.TYPE].equals(types[i]))
               typeLabel.setText(languageTypes[i]);

       titleLabel.setText(infoString[AcornsProperties.TITLE]);
       nameField.setText(infoString[AcornsProperties.NAME]);
       descriptionLabel.setText(infoString[AcornsProperties.DESC]);

       int layerNo;
       if (infoString.length >= AcornsProperties.TYPES)
       {  for (int i=AcornsProperties.LAYERNAMES; i<infoString.length; i++)
          {   layerNo = i - AcornsProperties.LAYERNAMES;
              layerNames[layerNo].removeDocumentListener(this);
              layerNames[layerNo].setText(infoString[i].trim());
              layerNames[layerNo].addDocumentListener(this);
          }
       }
       nameField.getDocument().addDocumentListener(this);
       
   }    // End of setPanelInfo()
   
   /** Return lesson header information in preparation for a modify operation
    */
   public boolean modifyPanel()   {  return dialog(getPanelInfo());  }
   
   // Lesson Panel Dialog method.
   private boolean dialog(String[] infoString)
   {
      // Create the dialog panel and set its layout.
      JPanel dialogPanel = new JPanel();
      dialogPanel.setLayout(new GridLayout(20,1));
      
      // Create components for the dialog window.
      // Insert: Create title and combo box with lesson type selections.
      // Modify: Create title and label with lesson type.
      int type = 0;
      final String  dialogTitle;
      if (infoString==null)
      {
	 dialogTitle  = LanguageText.getMessage("acornsApplication", 17);
         infoString = new String[AcornsProperties.TYPES];
         for (int i=0; i<AcornsProperties.TYPES; i++) infoString[i] = "";
         
         comboBox = new JComboBox<String>(languageTypes);
         comboBox.setMaximumRowCount(20);
         comboBox.setToolTipText
                        (LanguageText.getMessage("acornsApplication", 18));
         dialogPanel.add(comboBox);
      }
      else
      {   dialogTitle  = LanguageText.getMessage("acornsApplication", 19);
          infoString = getPanelInfo();
	  for (int i=1; i<types.length; i++)
	  {  if (types[i].equals(infoString[AcornsProperties.TYPE])) type = i;
          }

         final JLabel jType = new JLabel(languageTypes[type]);
         dialogPanel.add(jType);
      }
      
      // Create the other components for the dialog window.
      final JTextField jTitle  = new JTextField
              (infoString[AcornsProperties.TITLE], MAX_FIELD);
      jTitle.setToolTipText(LanguageText.getMessage("acornsApplication", 21));
      jTitle.setFont(new Font(null, Font.PLAIN, 12));
		
      final JTextField jLink = new JTextField(infoString[AcornsProperties.NAME]
                             , LessonPanel.MAX_FIELD);
      jLink.setToolTipText(LanguageText.getMessage("acornsApplication", 23));
      jLink.setFont(new Font(null, Font.PLAIN, 12));
		
      final JTextField jDescription
              = new JTextField(infoString[AcornsProperties.DESC], MAX_FIELD);
      jDescription.setToolTipText
                           (LanguageText.getMessage("acornsApplication", 25));
      jDescription.setFont(new Font(null, Font.PLAIN, 12));
	
      final JLabel jLayersLabel
              = new JLabel(LanguageText.getMessage("acornsApplication", 26));

      LayerPanel[] jLayerNames = new LayerPanel[AcornsProperties.MAX_LAYERS];
      String[] defaultLayerNames = Environment.getDefaultLayerNames();

      for (int i=0; i<AcornsProperties.MAX_LAYERS; i++)
      {   
          jLayerNames[i] = new LayerPanel("" + (i+1), defaultLayerNames[i], 12);
      }

      // Add other components to the dialog panel.
      String title= LanguageText.getMessage("acornsApplication", 20);
      dialogPanel.add(new JLabel(title));
      dialogPanel.add(jTitle);
 
      String descLabel = LanguageText.getMessage("acornsApplication", 24);
      dialogPanel.add(new JLabel(descLabel));
      dialogPanel.add(jDescription);

      String linkLabel = LanguageText.getMessage("acornsApplication", 22);
      dialogPanel.add(new JLabel(linkLabel));
      dialogPanel.add(jLink);
      dialogPanel.add(jLayersLabel);

      for (int i=0; i<jLayerNames.length; i++)  dialogPanel.add(jLayerNames[i]);

      // Get lesson header information.
      boolean validEntryNeeded = true;

      JFrame root = Environment.getFrame();
      while (validEntryNeeded)
      {  int result = JOptionPane.showConfirmDialog(root, dialogPanel
		         , dialogTitle
		         , JOptionPane.YES_OPTION, JOptionPane.PLAIN_MESSAGE);
							 
         if (result != JOptionPane.YES_OPTION) return false;
		
         if (comboBox!=null) lessonType  = comboBox.getSelectedIndex();

         title = jTitle.getText();
         validEntryNeeded=false;
         if (title.length() == 0) validEntryNeeded = true;
         if (lessonType == 0)     validEntryNeeded = true;
         if (validEntryNeeded)
         {   result = JOptionPane.showConfirmDialog(root
	                , LanguageText.getMessage("acornsApplication", 28)
	                , LanguageText.getMessage("acornsApplication", 29)
	                , JOptionPane.CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
             if (result != JOptionPane.YES_OPTION) return false;
         }   // End if.
      }      // End while

      // Create lesson panel to hold the header information.
      infoString = new String[AcornsProperties.TYPES];
      infoString[AcornsProperties.TYPE]  = types[lessonType];
      infoString[AcornsProperties.TITLE] = jTitle.getText();
      infoString[AcornsProperties.NAME]  = jLink.getText();
      infoString[AcornsProperties.DESC]  = jDescription.getText();
  
      for (int i=0; i<AcornsProperties.MAX_LAYERS; i++)
      { infoString[i+AcornsProperties.LAYERNAMES]  = jLayerNames[i].getText(); }
      setPanelInfo(infoString);
      return true;
  
   }  // End of dialog()

   /** Listen for changes to the text field */
   public void insertUpdate(DocumentEvent e)  { setDirty(); }
   public void removeUpdate(DocumentEvent e)  { setDirty();  }
   public void changedUpdate(DocumentEvent e) { setDirty(); }
       
   /** Method to set active lesson dirty */
   private void setDirty()
   {
      FileObject active = Environment.getActiveFile();
      if (active==null) return;
          
      Lesson lesson = active.getActiveLesson();
      if (lesson==null) return;
          
      lesson.setDirty(true);
   }    // End of setDirty()
   
   /** GUI class to hold layer information */
   class LayerPanel extends JPanel
   {  
	 private final static long serialVersionUID = 1;
	   
 	 private JTextField layerField;
      
      public LayerPanel(String label, String layer, int fontSize)
      {   setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

          if (label.length()==1) label = " " + label;
          JLabel labelField = new JLabel(label);
          add(labelField);
          add(Box.createHorizontalStrut(5));
          
          layerField = new JTextField(layer, MAX_FIELD);
          layerField.setFont(new Font(null, Font.PLAIN, fontSize));
          add(layerField);
      }

      public String getText()  { return layerField.getText().trim(); }
      public void setText(String layer) { layerField.setText(layer); }
      
      public void addDocumentListener(DocumentListener listener) 
      {  layerField.getDocument().addDocumentListener(listener); }
      
      public void removeDocumentListener(DocumentListener listener)
      {  layerField.getDocument().removeDocumentListener(listener);  }
   }  // End of LayerPanel class
}     // End of LessonPanel class
