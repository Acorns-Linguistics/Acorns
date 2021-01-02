/*
 * InvalidFileTypeException.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package org.acorns;

/** Define exception thrown when detecting a non Acorns file type
 */
public class InvalidFileTypeException extends Exception
{
     private static final long serialVersionUID=1L;

    /**
     * Constructor that instantiates an InvalidFileTypeException object
     */
    public InvalidFileTypeException()
	   {
	       super (org.acorns.language.LanguageText.getMessage
                                                     ("acornsApplication", 12));
	   }
}  
