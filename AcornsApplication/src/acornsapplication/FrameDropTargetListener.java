/**
 * FrameDropTarget.java
 * @author HarveyD
 * @version 4.10 Beta
 *
 * Copyright 2007-2015, all rights reserved
 */

package acornsapplication;

import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import org.acorns.lesson.*;
import org.acorns.*;

public class FrameDropTargetListener implements DropTargetListener
{
    public void drop(DropTargetDropEvent dtde)
    {   dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        File[] files = getTransferObjects(dtde.getTransferable());
        if (files == null) 
        { dtde.dropComplete(false);
          return;
        }
        String extension;
        String[] args = new String[1];

        for (int i=0; i<files.length; i++)
        {
              args[0] = files[i].getPath();
              extension = args[0].substring(args[0].lastIndexOf(".")+1);
              if (isImage(extension))
              {   InsertImage insert = new InsertImage();
                  Environment.setText(insert.processOption(args));
              }
              else if (extension.toLowerCase().equals("lnx"))
              {
                 FileOpen open = new FileOpen();
                 Environment.setText(open.processOption(args));
              }
              else if (extension.toLowerCase().equals("xml"))
              {
                 FileImport importFile = new FileImport();
                 Environment.setText(importFile.processOption(args));
              }
              AcornMenu.setEnable();          // Activate the icons
        }
        dtde.dropComplete(true);
    }
    public void dragEnter (DropTargetDragEvent dtde) 
    {
        if (!acceptIt(dtde.getTransferable())) dtde.rejectDrag();
        else dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
    }

    public void dragExit (DropTargetEvent dte) {}

    public void dragOver (DropTargetDragEvent dtde)
    {   if (!acceptIt(dtde.getTransferable())) dtde.rejectDrag();
        else dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
    }

    public boolean isDropAcceptable(DropTargetDropEvent dtde)
    {  return acceptIt(dtde.getTransferable());  }

    public boolean isDragAcceptable(DropTargetDragEvent dtde)
    { return acceptIt(dtde.getTransferable());
    }
  
    public void dropActionChanged (DropTargetDragEvent dtde)
    {   if (!acceptIt(dtde.getTransferable())) dtde.rejectDrag(); }

    /** Method to determine if drop type is correct
     *
     * @param transfer The transferable object
     * @return true if acceptable drop type.
     */
    private boolean acceptIt(Transferable transfer)
    {
        DataFlavor[] flavors = transfer.getTransferDataFlavors();
        for (int i=0; i<flavors.length; i++)
        { if (flavors[i].getRepresentationClass() == List.class) return true;
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
        AbstractList<?> list = null;

        for (int i=0; i<flavors.length; i++)
        {  if (flavors[i].getRepresentationClass() == List.class)
                listFlavor = flavors[i];
           if (flavors[i].getRepresentationClass() == AbstractList.class)
                listFlavor = flavors[i];
        }

        try
        {  if (listFlavor!=null)
           {
               list = (AbstractList<?>)transfer.getTransferData(listFlavor);

               int size = list.size();
               file = new File[size];
               String extension;
               AppObject active = Files.getActiveFile();

               Lesson lesson = null;
               if (active!=null) lesson = active.getActiveLesson();

               for (int i=0; i<size; i++)
               {
                  file[i] = (File)list.get(i);
                  extension = file[i].getName();
                  extension = extension.substring(extension.lastIndexOf(".")+1);

                 if (isImage(extension))
                 {  if (lesson == null) return null;  }
                 else if (extension.toLowerCase().equals(".lnx")
                         || extension.toLowerCase().equals(".xml"))
                 {  if (Files.isMaxFilesOpen()) return null;  }
               }  // End for
           }
        }
        catch (Throwable e)  {  return null;  }
        return file;
    }   // End acceptIt()

    /** Determine if an extension goes with
     *
     * @param name of file to check
     * @return true if an image file, false otherwise.
     */
    private boolean isImage(String extension)
    {
        String[] imageArray = ImageIO.getReaderFormatNames();
        for (int i=0; i<imageArray.length; i++)
            if (extension.equals(imageArray[i])) return true;

        return false;
    }
}        // End of FrameDropTarget class
