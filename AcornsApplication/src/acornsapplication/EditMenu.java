/*
 * EditMenu.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.event.*;

import javax.swing.*;
import org.acorns.language.*;

public class EditMenu extends AcornMenu
{
   JMenuItem editUndo, editRedo, soundEditor, editCopy, editPaste, movieToGif;
	
	private static final long serialVersionUID = 1;
	  
   // Constructor for the File menu bar.
   public EditMenu(JToolBar toolbar, JMenuBar bar)
   {
      super("Edit;"+LanguageText.getMessage("acornsApplication", 84), 'e');

      String[] msg = LanguageText.getMessageList("acornsApplication", 84);
      
      editUndo    = menuItem(toolbar, "Undo;"+msg[1], 'u', "undo.png", UNDO);
      setShortcut(editUndo, KeyEvent.VK_Z);
      editRedo   = menuItem(toolbar, "Redo;"+msg[2], 'r', "redo.png", REDO);
      setShortcut(editRedo, KeyEvent.VK_Y);
      
      addSeparator();
      soundEditor = menuItem
              (toolbar, "Sound Editor;"+msg[3], 'E', "soundeditor.png", ALWAYS);
      
      // Had to disable because javafx is now detached from the jdk
       movieToGif = menuItem
              (toolbar, "Movie to Animation", 'M', "video.png", ALWAYS);
       addSeparator();
      
      new InsertMenu(toolbar, this);
      new DeleteMenu(toolbar, this);
      new ModifyMenu(toolbar, this);

      addSeparator();
      editCopy = menuItem(toolbar, "Copy Lesson;"+msg[4], 'C', null, LESSON);
      setShortcut(editCopy, KeyEvent.VK_C);
      editPaste 
          = menuItem(toolbar, "Paste Lesson;"+msg[5], 'P', null, PASTE + OPEN);
      setShortcut(editPaste, KeyEvent.VK_V);
      bar.add(this);
      setEnableStatus();
   }
	
   // Implement action listener for this menu.
   public void actionPerformed(ActionEvent ae)
   {  
      processCommand(ae);
      setEnableStatus();
   }
}