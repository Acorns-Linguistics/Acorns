/*
 * FileMenu.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.event.*;
import javax.swing.*;

import org.acorns.audio.SoundDefaults;
import org.acorns.language.*;

public class FileMenu extends AcornMenu 
{

   private final static long serialVersionUID = 1;
	  
   String[] recentFiles;
   int      recentFileIndex;
   JToolBar toolbar;
   String[] msg;
   
   // Constructor for the File menu bar.
   public FileMenu(JToolBar toolbar, JMenuBar bar)
   {  
      super("File;" + LanguageText.getMessage("acornsApplication", 58), 'f');
      this.toolbar = toolbar;
      AppEnv.setRecentFileMenu(this);

      msg = LanguageText.getMessageList("acornsApplication", 58);
      
      JMenuItem fileNew    
              = menuItem(toolbar, "New;"+msg[1], 'n', "new.png", ALWAYS);
      setShortcut(fileNew, KeyEvent.VK_N);
      JMenuItem fileOpen   
              = menuItem(toolbar, "Open;"+msg[2], 'o', "open.png", ALWAYS);
      setShortcut(fileOpen, KeyEvent.VK_O);
      menuItem(toolbar, "Close;" +msg[3], 'c', "close.png", OPEN);
      addSeparator();
      JMenuItem fileSave   
              = menuItem(toolbar, "Save;"+msg[4], 's', "save.png", OPEN);
      setShortcut(fileSave, KeyEvent.VK_S);
      menuItem(toolbar, "Save As;"+msg[5], 'a', "saveas.png", OPEN);
      addSeparator();
      menuItem(toolbar, "Export;"+msg[6], 'e', "export.png", OPEN);
      menuItem(toolbar, "Import;"+msg[7], 'i', "import.png", ALWAYS);
      addSeparator();
      menuItem(toolbar, "Make Web Page;"+msg[8], 'w', "web.png", LESSON);
      menuItem(toolbar, "Make Mobile App;"+msg[13], 'm', "mobile.png", LESSON);
      addSeparator();
	  menuItem(toolbar, "Properties;"+msg[9], 'r', "blank.png", OPEN);
      menuItem(toolbar, "Print Preview;"+msg[10], 'v', "printpreview.png", OPEN);
      JMenuItem filePrint  
              = menuItem(toolbar, "Print;"+msg[11],  'p', "print.png", OPEN);
      setShortcut(filePrint, KeyEvent.VK_P);
      addSeparator();
      recentFileIndex = getItemCount();
      addFileItems();
	  setEnableStatus();
	  bar.add(this);
   }
   
   // Implement action listener for this menu.
   public void actionPerformed(ActionEvent ae)
   {  
       String result = processCommand(ae);
       boolean type   = Character.isDigit(result.charAt(0));
       if (type)
       {
          int      option = Integer.parseInt(result.substring(0,1));

          String[] args = new String[1];
          args[0] = recentFiles[option-1];
          
          result = processClassInstance("File", "Open", args);
          AppEnv.setText(result);
       }
       setEnableStatus();
   }
   
   /** Update the recent files on the menu */   
   public final void addFileItems()
   {
      // Remove recent files from the menu.
      while (getItemCount() > recentFileIndex)
      {  this.remove(recentFileIndex);  }

      recentFiles = AppEnv.getRecentFiles();
      JMenuItem[] items     = new JMenuItem[recentFiles.length];

      // Create array of menu items.
      boolean anyFiles = false;
      String  menuText;
      for (int i=0; i<recentFiles.length; i++)
      {
         if (recentFiles[i] != null)
         {   
        	 menuText = "" + (i+1) + " " + SoundDefaults.normalizeFilePath(recentFiles[i]);
             items[i] = menuItem(toolbar, menuText, Character.forDigit(i+1,10)
                   , null, ALWAYS);
             anyFiles = true;
         }
      }
      if (anyFiles) addSeparator();
      JMenuItem fileExit = menuItem(toolbar, "Exit;"+msg[12],'x', null, ALWAYS);
      setShortcut(fileExit, KeyEvent.VK_Q);
   }
}
