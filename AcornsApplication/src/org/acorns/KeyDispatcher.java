/*
 * KeyDispatcher.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package org.acorns;

import java.awt.*;
import java.awt.event.*;

/** Handle key events for the Acorns application. The KeyEvent listener
 *  is insufficient because it does not work when the application frame loses
 *  focus
 */
public class KeyDispatcher implements KeyEventDispatcher
{
  public KeyDispatcher()
  {   KeyboardFocusManager manager 
           = KeyboardFocusManager.getCurrentKeyboardFocusManager();
      manager.addKeyEventDispatcher(this);
  }
  
   /** Process events associated with user key depressions
    *
    * @param event The object thrown when a key press occurs
    */
   public boolean dispatchKeyEvent(KeyEvent event)
	  {
	     if (event.getID() != KeyEvent.KEY_PRESSED) { return false; }
		
	     FileObject active = Environment.getActiveFile();
		    boolean ok        = false;
	     int     keyCode   = event.getKeyCode();
		
		    switch (keyCode)
		    {
		        case KeyEvent.VK_HOME:
			            if (active!=null) ok = active.setActiveLesson(false, true);
				           break;
			       case KeyEvent.VK_END:
			            if (active!=null) ok = active.setActiveLesson(true, true);
				           break;
			       case KeyEvent.VK_PAGE_UP:
			            if (active!=null) ok = active.setActiveLesson(false, false);
				           break;
			       case KeyEvent.VK_PAGE_DOWN:
			            if (active!=null) ok = active.setActiveLesson(true, false);
				           break;
			       case KeyEvent.VK_ESCAPE:
               ok = Environment.exitPlayMode();
               break;
          default: return false;
		    }	
		    if (!ok) Toolkit.getDefaultToolkit().beep();
	     return true;
	  }
}