/*
 * LessonTypes.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */

package org.acorns;

import org.acorns.lesson.*;

import java.lang.reflect.InvocationTargetException;

import org.acorns.language.*;

public class LessonTypes
{
    private static Class<?>[] lessonTypes = null;
  	
	 /** Determine the list of available lesson types 
     *  @return false if none available.
     */
     public static boolean setAvailableLessonTypes(String[] args)
     {
        if (lessonTypes!=null) return true;
         
        Class<?>   className;
        
        int     tablePointer = 0;
        Class<?>[] lessonTable  = new Class[args.length];
        for (int a=0; a<args.length; a++)
        {
            try
            {   // Try to locate the class name.
                className = Class.forName(args[a]);

                // Try to instantiate the class.
                className.getDeclaredConstructor().newInstance();
                
                lessonTable[tablePointer] = className;      
                tablePointer++;
            }
            catch (Throwable t) 
            { 
                Environment.setText
                    (LanguageText.getMessage("acornsApplication", 30, args[a]));
            }
        }
   
        if (tablePointer==0) return false;
        
        lessonTypes  = new Class[args.length];
        System.arraycopy(lessonTable, 0, lessonTypes, 0, tablePointer);
        return true;
     }    // End of setAvailableLessonTypes.
     
     public static void setAvailableLessonTypes(Class<?>[] lessonClasses)
     {  lessonTypes = lessonClasses; }

   /** Get the list of possible lesson object types
    *
    * @return An array of lesson objects
    */
    public static Lesson[] getAvailableLessonObjects() 
    {
       Lesson[] availableTypes = new Lesson[lessonTypes.length];
       for (int t=0; t<lessonTypes.length; t++)
       {  try
          {  // Try to instantiate the class.
             availableTypes[t] = (Lesson)lessonTypes[t].getDeclaredConstructor().newInstance();
          }
          catch (Throwable throwIt)
          {  Environment.setText
                     (LanguageText.getMessage("acornsApplication", 31));
          }           
       }
       return availableTypes;
   }  // End of getAvailableLessonObjects().
    
    /** Return the list of available lesson type class objects */
    public static Class<?>[] getAvailableLessonTypes()
    {
        return lessonTypes;
    }

    /** Method to instantiate a blank lesson
     *
     * @return Instantiated lesson
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static Lesson getBlankLesson()
            throws InstantiationException, IllegalAccessException,
                   NoSuchMethodException, InvocationTargetException
    {
        Lesson instance = (Lesson)lessonTypes[0].getDeclaredConstructor().newInstance();
        return instance;
    }
}   // End of lessonTypes class
