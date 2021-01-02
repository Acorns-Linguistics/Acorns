/*
 * Acorn.java
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
import java.io.*;

import org.acorns.*;
import org.acorns.language.*;

//--------------------------------------------------------------
// Main Class that instantiates the JFrame
//--------------------------------------------------------------
public class Acorn implements WindowListener
{
   //--------------------------------------------------------------
   // Constructor for primary application window frame.
   //--------------------------------------------------------------
   public Acorn(String[] args) throws FileNotFoundException
   {
      // Register the property change listeners
      new Properties();
       
      // Initialize the application environment.
      new AppEnv(args);
              
      // Get the primary application frame
      JFrame frame = AppEnv.getFrame();
      Container container = frame.getContentPane();
       
      // Create menu components in the north portion of the main frame.
      JToolBar   toolbar = new JToolBar(JToolBar.HORIZONTAL);
      toolbar.setBorder(null);
      toolbar.setMargin(new Insets(0,0,0,0));
      JMenuBar   bar     = new JMenuBar();

      // Configure the menu bar.
      new FileMenu(toolbar,   bar);
      new EditMenu(toolbar,   bar);
      new WindowMenu(toolbar, bar);
      new ToolsMenu(toolbar,  bar);
      new HelpMenu(toolbar,   bar);

      // Create panel to hold the menubar and the toolbar.
      frame.setJMenuBar(bar);
      container.add(toolbar, BorderLayout.NORTH);
     
      // Make everything visible.
      frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      addListeners(frame);
      
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
              frame.pack();
              frame.setVisible(true);
              frame.setLocationRelativeTo(null);
          }
      });

   }   // Frame Create method.
   
   private void addListeners(JFrame frame)
   {
      frame.addWindowListener(this);
   }
   
   //--------------------------------------------------------------
   // Methods to listen for the closing of the frame window.   
   //--------------------------------------------------------------
   public void windowClosing(WindowEvent event)
   {
       boolean answer = AppEnv.shutDown();
       Window  window = event.getWindow();
       if (answer) 
       {
           window.dispose();
           System.exit(0);
       }
       else
       {
           window.setVisible(true);
           Frame frame = Environment.getRootFrame();
           window.setLocationRelativeTo(frame);
           AppEnv.setText(LanguageText.getMessage("acornsApplication", 57));
       }
   }
   
   //--------------------------------------------------------------
   // Unused window methods.
   //--------------------------------------------------------------
    public void windowDeactivated( WindowEvent event ) {}
    public void windowActivated(   WindowEvent event ) {}
    public void windowDeiconified( WindowEvent event ) {}
    public void windowIconified(   WindowEvent event ) {}
    public void windowClosed(      WindowEvent event ) {}
    public void windowOpened(      WindowEvent event ) {}
}

