/*
 * ModifyLesson.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.io.*;
import org.acorns.language.*;

/*
 *  This class processees commands that modify lesson header information.
 */
public class ModifyLesson extends MenuOption
{
   /*
    * This is a polymorphic method that processes user commands
    *
    * @param status The Object controlling the Acorns environment
    * @param args   An array of user arguments needed to process this command
    */
   public String processOption(String[] args)
   {
      AppObject active = Files.getActiveFile();

      try
      {   if (active.modifyLesson())
          return LanguageText.getMessage("acornsApplication", 46);
          else return LanguageText.getMessage("acornsApplication", 45);
      }
      catch (IOException exception)  {  return exception.toString();  }
  } // End of processOption method.
}    // End of ModifyLesson class.
