/*
 * FileExit.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import org.acorns.language.*;

public class FileExit extends MenuOption
{
   public String processOption(String[] args)
	  {
	      boolean answer = AppEnv.shutDown();
		     if (answer) System.exit(0);
		
		     return LanguageText.getMessage("acornsApplication", 57);
	  }
}