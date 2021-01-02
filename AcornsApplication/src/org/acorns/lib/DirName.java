/**
 * DirName.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package org.acorns.lib;

import java.io.*;

/**
 *  This class filters class directory names starting with a designated string.
 *  The Lesson class uses this filter to find polymorphic classes that
 *  implement lesson types. All lesson type directories start with the
 *  String, "Lessons".
 */
public class DirName implements FilenameFilter
{
    String pattern;
    
   /**
    *  Set the pattern of matcing directory names. 
    *
    * @param pattern Matching class files must start with this string. 
    *
    * <p>For example, if the pattern is "Lessons", this filter
    * will search for all directories beginning with "Lessons"</p>
    *
    */
    public DirName(String pattern)    { this.pattern = pattern; }
    
   /**
    * <p>Determine whether a file in a directory is a valid directory</p>>
    *
    * @param dir Qualified path to a directory
    * @param name Name of a file in a directory
    */
    public boolean accept(File dir, String name)
   {
      File path = new File(dir + System.getProperty("file.separator") + name);
      
      if (!path.isDirectory())       return false;
      if (!name.startsWith(pattern)) return false;
      return true;
   }
}
