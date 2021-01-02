/*
 * EditPasteLesson.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.lang.reflect.*;
import javax.swing.*;
import java.util.*;

import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.language.*;

public class EditPasteLesson extends MenuOption
{
   public String processOption(String[] args)
   { 
       FileObject active = Files.getActiveFile();
       if (active == null) 
           return LanguageText.getMessage("acornsApplication", 61);
       try
       {   AcornsTransfer transfer = Lesson.getCopyLesson();
           if (transfer==null) 
               return LanguageText.getMessage("acornsApplication", 77);

           Lesson lesson = transfer.getLesson();
           Lesson newLesson = switchLessonTypes(lesson);
           if (newLesson==null)
               return LanguageText.getMessage("acornsApplication", 70);

           
           String[] panelInfo = transfer.getLessonHeader();
           String[] newPanel = new String[panelInfo.length];
           newPanel[0] = newLesson.getName();
           System.arraycopy(panelInfo, 1, newPanel, 1, newPanel.length-1);
           if (active.newLesson(newPanel, newLesson))
		              return LanguageText.getMessage("acornsApplication", 78);
           else return LanguageText.getMessage("acornsApplication", 79);
       }
       catch (Exception exception)
       {
          return LanguageText.getMessage("acornsApplication", 3);
       }
    }

    /** Method to switch lesson type to another in same category
     *
     * @param lesson The lesson whose type should be switched
     * @return The instantiated lesson of the new type
     */
    private static Lesson switchLessonTypes(Lesson lesson)
    {
       Method getCategory;
       String categoryName;

       Class<?>[] lessonTypes = LessonTypes.getAvailableLessonTypes();
       Class<?> lessonClass = lesson.getClass();
       try
       {
          getCategory = lessonClass.getMethod("getCategory");
          categoryName
              = (String)getCategory.invoke(lesson, new Object[0]);
       }
       catch (Exception e)  { return lesson; }

       // Create array of choices for lesson type.
       ArrayList<String> types = new ArrayList<String>();
       types.add(LanguageText.getMessage("acornsApplication", 80));
       Lesson[] instances = new Lesson[lessonTypes.length];
       int count = 0;

       Class<?>[] paramClass = new Class[] { Object.class };
       Object[] params = new Object[] { lesson };

       Lesson newLesson;
       Constructor<?> constructor;
       String category;
       for (int i=0; i<lessonTypes.length; i++)
       {  try
          {  
             constructor = lessonTypes[i].getConstructor(paramClass);
             newLesson = (Lesson)constructor.newInstance(params);

             getCategory = newLesson.getClass().getMethod("getCategory");
             category = (String)getCategory.invoke(newLesson, new Object[0]);
             if (category.equals(categoryName))
             {   instances[count++] = newLesson;
                 types.add(newLesson.getName());
             }
           }
           catch (Exception e)  { }
      }
      if (count<=1) return lesson;

      String message = LanguageText.getMessage("acornsApplication", 81);
      String answer = types.get(0);
      while (types.get(0).equals(answer))
      {
         answer = (String)JOptionPane.showInputDialog
              (null, message, LanguageText.getMessage("acornsApplication", 82)
              , JOptionPane.PLAIN_MESSAGE
              , Environment.getIcon(AcornsProperties.ACORN,20)
              , types.toArray(), 0);

         if (answer==null) return null;
      }

      for (int i=0; i<count; i++)
      {  if (answer.equals(types.get(i+1))) return instances[i]; }
      return lesson;
    } // End switchLessonTypes()
}
