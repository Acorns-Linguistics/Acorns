/*
 * AcornMenu.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.net.*;
import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.audio.*;
import org.acorns.language.*;

/**
 * An abstract polymorphic class common to all Acorns menus
 */
   public abstract class AcornMenu extends JMenu implements ActionListener
   {
	  private final static long serialVersionUID = 1;
      private final static int ICON_SIZE = 25;
	
      /** Always enable this menu option */
      protected final static int ALWAYS = 1;
      /** Enable this menu option when files are open */
      protected final static int OPEN   = 2;
      /** Enable this option when a file contains at least one lesson */
      protected final static int LESSON = 4;
      /** Enable when there are undone lesson operations that can be redone */
      protected final static int REDO   = 8;
      /** Enable whenever lesson operations can be undone */
      protected final static int UNDO   = 16;
      /** Enable whenever a lesson was copied and ready to paste */
      protected final static int PASTE  = 32;
	
      private   String menuName;

      // menu Items and labels that are sometimes disabled.
      private static Vector<JButton>   buttons;
      private static Vector<JMenuItem> menuItems;
      private static Vector<Integer>   enableFlags;

   /**
    * Method to instantiate a menu that will contain a group of options
    *
    *@param menuName A string designating the name of this menu
    *@param mnemonic A shortcut key that triggers this menu
    * environment
    */
   public AcornMenu(String name, char mnemonic)
   {
      super();
      setMnemonic(mnemonic);

	
      String[] nameData = name.split(";");
      menuName = nameData[0];

      if (nameData.length==1) setText(nameData[0]);
      else setText(nameData[1]);

      if (buttons==null)     buttons       = new Vector<JButton>();
      if (menuItems==null)   menuItems     = new Vector<JMenuItem>();
      if (enableFlags==null) enableFlags   = new Vector<Integer>();
   }  // End of constructor
	
	  /**
    * Method to configure the icon button for the toolbar and the 
    * menu list for this menu item.
    *
    *@param toolbar The toolbar that this item will be part of
    *@param text The text indicating this menu's option and tool tip
    *@param c The shortcut character
    *@param icon The name of the icon that will display on the toolbar
    *@param whenToEnable A symbol indicating when enable this item
    */
    public JMenuItem menuItem(JToolBar toolbar, String text, char c
                                            , String icon, int whenToEnable) 
    {
        JMenuItem item;
        ImageIcon image = null;
        Image     newImage;
        JButton   button = null;

        String[] textData = text.split(";");
        if (!Character.isDigit(text.charAt(0)))
        {   if (textData.length==1) text = textData[0];
            else text = textData[1];
        }

        if (icon!=null)
        {
           URL url = getClass().getResource("/data/" + icon);
           newImage  = Toolkit.getDefaultToolkit().getImage(url);
           newImage = newImage.getScaledInstance
                        (ICON_SIZE, ICON_SIZE,Image.SCALE_REPLICATE);
           image = new ImageIcon(newImage);
        }

        if (icon!=null && !icon.equals("blank.png"))
        {
            // Configure the tool bar button.
            button = new JButton(image);
            button.setName("");
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setVerticalTextPosition(JButton.BOTTOM);
            button.setHorizontalTextPosition(JButton.CENTER);
            button.setToolTipText(text);
            button.setActionCommand(textData[0]);
            button.setBounds(0,0,50,50);
            button.addActionListener(this);
            toolbar.add(button);
            item = new JMenuItem(text, image);
        }

        if (icon!=null)  item = new JMenuItem(text, image);
        else             item = new JMenuItem(text);

        // Add item to the menu bar.
        if (c!='\0') item.setMnemonic(c);
        item.setActionCommand(textData[0]);
        add(item);
        item.addActionListener(this);

        // Handle icons and buttons that can optionally be disabled.
        if (whenToEnable != ALWAYS)
        {
           buttons.add(button);
           menuItems.addElement(item);
           enableFlags.addElement(whenToEnable );
        }
        return item;
  }   // End of menuItem
	
   /**
    * Method to use the enable or disable menu options based on the system 
    * state
    *
    */
    public void setEnableStatus()  {  AcornMenu.setEnable();  }
	
   /**
    * <p>Method to use to enable or disable menu options based on the system state</p>
    *
    */
    public static void setEnable()
    {
         Integer   enableFlag;
         int       flag;
         JButton   button;
         JMenuItem menuItem;
         int       whenToEnable = 0;

         FileObject active = Files.getActiveFile();
         whenToEnable = ALWAYS;
         if (active!=null)
         {
             whenToEnable |= OPEN;
             Lesson lesson = active.getActiveLesson();
             if (Lesson.isPastable()) whenToEnable |= PASTE;
             if (lesson!=null)
             {
                 whenToEnable |= LESSON;
                 if (!lesson.isRedoEmpty()) whenToEnable |= REDO;
                 if (!lesson.isUndoEmpty()) whenToEnable |= UNDO;
             }
         }

         for (int i=0; i<enableFlags.size(); i++)
         {
            enableFlag = enableFlags.elementAt(i);
            flag       = enableFlag.intValue();
            if (menuItems.elementAt(i)!= null)
            {
                button     = buttons.elementAt(i);
            }
            else  button     = null;

            menuItem   = menuItems.elementAt(i);

            if ((flag & whenToEnable) == flag)
            {
               if (button != null) button.setEnabled(true);
               menuItem.setEnabled(true);
            }
            else
            {
               if (button != null) button.setEnabled(false);
               menuItem.setEnabled(false);
            }
         }	  // End for
    }             // End of setEnable()
		
    /**
     * Method to respond to menu selection mouse clicks. The appropriate
     * menu class calls this method to complete the processing.
     *
     *@param ae Event object instantiated through a user mouse click
     */
     protected String processCommand(ActionEvent ae)
     {
       // Get command to process.
       String command = ae.getActionCommand();

       // Process command if it doesn't start with a digit.
       // If it starts with a digit, return to the menu for special processing.
       char character = command.charAt(0);
       if (Character.isDigit( character ))
            return command;
       else return processClassInstance(menuName, command, null);
    }  // End of processCommand
	
    /**
     * Method common to menus to facilitate processing user commands
     *
     *@param menuName Name of this menu
     *@param command Name of the option on this menu to proces
     *@param args An array of menu arguments needed to process this selection
     */
     protected String processClassInstance(String menuName, String command, String[] args)
     {
          String classString = command.replaceAll(" ","");
          String result;
          Environment.setText
               (LanguageText.getMessage("acornsApplication", 42, menuName));
		
          try
          {
              PlayBack.stopPlayBack();
              Class<?> className = Class.forName
                   ("acornsapplication." + menuName + classString);
              MenuOption classInstance = (MenuOption)className.getDeclaredConstructor().newInstance();
              result = classInstance.processOption(args);
 	      Environment.setText(result);
          }
          catch (Throwable t)
          {
              if (Character.isDigit(command.charAt(0))) return command;
              else
              {  result = "Error: " + command + " " + t;
                 Environment.setText(result);
                 t.printStackTrace();
              }
          }
          return result;
     }    // End of processClassInstance
     
   /** Method to set shortcut keys 
    * 
    * @param menuItem The menu item to attach the shortcut key
    * @param keyEvent The shortcut key
    */
   protected void setShortcut(JMenuItem menuItem, int keyEvent)
   {
	   menuItem.setAccelerator(KeyStroke.getKeyStroke(keyEvent, ActionEvent.CTRL_MASK));
   }
	
   /**
     * Polymorphic method to process menu options
     */
   public abstract void actionPerformed(ActionEvent ev);  
    
}   // End of AcornMenu class