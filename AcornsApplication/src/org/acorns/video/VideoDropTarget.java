/**
 * VideoDropTarget.java
 * @author HarveyD
 * @version 6.00
 *
 * Copyright 2007-2015, all rights reserved
 */

package org.acorns.video;

import java.lang.reflect.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

import java.io.*;
import java.util.*;

public class VideoDropTarget implements DropTargetListener
{
   Method videoDroppedMethod;
   Object dropObject;

   public VideoDropTarget(java.awt.Component panel, Object dropObject)
   {  new DropTarget(panel, this);
      this.dropObject = dropObject;
      Class<?> dropClass = dropObject.getClass();

      try
      {  videoDroppedMethod = dropClass.getMethod
                  ("videoDropped", new Class<?>[]{File.class} );
      }
      catch (NoSuchMethodException ex) {}
   }

   /** Method to process drops into the panel
    *
    * @param dtde The event triggering this method to execute
    */
   public void drop(DropTargetDropEvent dtde)
    {   dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        File[] files = getTransferObjects(dtde.getTransferable());
        if (files == null || files.length==0)
        { java.awt.Toolkit.getDefaultToolkit().beep();
          dtde.dropComplete(false);  return;
        }

        String arg = files[0].getPath();
        String extension = arg.substring(arg.lastIndexOf(".")+1);
        if (isVideo(extension))
        {  try  {  videoDroppedMethod.invoke(dropObject, files[0]); }
           catch (Exception ex)   {  dtde.dropComplete(false);  }
        }
        else { java.awt.Toolkit.getDefaultToolkit().beep();
               dtde.dropComplete(false);
               return;
             }
        dtde.dropComplete(true);
    }

    /** Method to determine if drags to this object are okay
     *
     * @param dtde The triggering event
     */
    public void dragEnter (DropTargetDragEvent dtde)
    {
        if (!acceptIt(dtde.getTransferable())) dtde.rejectDrag();
        else  dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
    }

    public void dragExit (DropTargetEvent dte) {}

    public void dragOver (DropTargetDragEvent dtde)
    {  if (!acceptIt(dtde.getTransferable())) dtde.rejectDrag();
       else dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
    }

    public void dropActionChanged (DropTargetDragEvent dtde)
    {   if (!acceptIt(dtde.getTransferable())) dtde.rejectDrag();
        else dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
    }

    /** Method to determine if drop type is correct
     *
     * @param transfer The transferable object
     * @return true if acceptible drop type.
     */
    private boolean acceptIt(Transferable transfer)
    {   DataFlavor[] flavors = transfer.getTransferDataFlavors();
        for (int i=0; i<flavors.length; i++)
        {  if (flavors[i].getRepresentationClass() == List.class) return true;
           if (flavors[i].getRepresentationClass() == AbstractList.class)
                return true;
        }
        return false;
    }

    /** Method to get the transferable list of files
     *
     * @param transfer The transferable object
     * @return An array of file objects or (null if none)
     */
    private File[] getTransferObjects(Transferable transfer)
    {
        DataFlavor[] flavors = transfer.getTransferDataFlavors();
        File[] file = new File[1];

        DataFlavor listFlavor = null;
        List<?> list = null;

        for (int i=0; i<flavors.length; i++)
        {  if (flavors[i].getRepresentationClass() == List.class)
           {    listFlavor = flavors[i]; break; }
           if (flavors[i].getRepresentationClass() == AbstractList.class)
           {    listFlavor = flavors[i]; break; }
        }

        try
        {  if (listFlavor!=null)
           {
               list = (List<?>)transfer.getTransferData(listFlavor);

               int size = list.size();
               file = new File[size];
               String extension;

               for (int i=0; i<size; i++)
               {
                  file[i] = (File)list.get(i);
                  extension = file[i].getName();
                  extension = extension.substring(extension.lastIndexOf(".")+1);

                 if (isVideo(extension)) return file;
               }  // End for
           }
        }
        catch (Throwable e)
        { return null; }
        return null;
    }   // End acceptIt()

    /** Determine if an extension goes with a valid video type
     *
     * @param name of file to check
     * @return true if an video file, false otherwise.
     */
    private boolean isVideo(String extension)
    {   String[] extensions
               = {"avi", "AVI", "mov", "MOV", "mpeg", "MPEG"
                       , "mp4", "mp1", "mp2", "mp3", "m4v", "mpg", "MPG"};

        for (int i=0; i<extensions.length; i++)
            if (extension.equals(extensions[i])) return true;

        return false;
    }
}        // End of VideoDropTarget class

