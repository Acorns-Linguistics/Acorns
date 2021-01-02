/*
 * NoDefaultNameException.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import org.acorns.language.*;
        
/** Exception thrown when the opened file type is not recognized
 */
public class NoDefaultNameException extends Exception
{
    private static final long serialVersionUID=1L;

    /** Constructor to create Exception object */
   public NoDefaultNameException()
	{
	   super (LanguageText.getMessage("acornsApplication", 40));
	}
}  
