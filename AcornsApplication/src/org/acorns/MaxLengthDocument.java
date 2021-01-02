/*
 * MaxLengthDocument.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package org.acorns;

import javax.swing.text.*;
import java.awt.*;

/** Set maximum length of text string entered into a TextField or TextArea */
public class MaxLengthDocument extends PlainDocument
{
   private final static long serialVersionUID = 1;
   int maxLength;
	
   /** Set maximum length of a TextField or TextArea */
   public MaxLengthDocument(int maxLength)
	  {   this.maxLength = maxLength;
	  }
	
   /** Insert string at end of text 
    *
    * @param offset Offset into the text string and check length
    * @param str object into to insert into
    * @param att Set of attributes
    */
	  public @Override void insertString(int offset, String str, AttributeSet att) 
	                                                         throws BadLocationException
	  {
	      if (getLength() + str.length() > maxLength)  
		          Toolkit.getDefaultToolkit().beep();
		     else super.insertString(offset, str, att);
	  }
	
   /** Replace string at in text and chheck length 
    *
    * @param offset Offset into the text string
    * @param length Length of string to replace
    * @param str Object into to insert into
    * @param att Set of attributes
    */
	  public @Override void replace(int offset, int length, String str, AttributeSet att) 
	                                                         throws BadLocationException
	  {
	     if (getLength() - length + str.length() > maxLength) 
		         Toolkit.getDefaultToolkit().beep();
	     else super.replace(offset, length, str, att);
	  }
}  // End of MaxLengthDocument.