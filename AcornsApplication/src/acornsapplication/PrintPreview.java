/**
   
 *   @author  harveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.border.*;
import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.language.*;

/** General class to generate a general Print Preview Panel */
public class PrintPreview extends JFrame 
{
    private static final long serialVersionUID=1L;

    private final static int START_SCALE = 10;  // Initially scale pages at 10% size.
	   private final static int MIN_SCALE   = 10;  // Set range of legal scale factors.
	   private final static int MAX_SCALE   = 100;
	   private final static int ICON_SIZE   =  20;
    
    private AcornPrintable target;    // The printable object.
    private JComboBox<String> previewScale;   // Combo box to determine view scale.
    private PreviewContainer preview; // Container for handling print previews.
    private int pageWidth;            // Size of print pages.
    private int pageHeight;
    private String[] msgs;

    /** Create panel using a object that defines what should print
     *
     * @param target Object defining what should print
     */
    public PrintPreview(AcornPrintable target) throws PrinterException
    {
       this(target, LanguageText.getMessage("acornsApplication", 136));
    }

    /** Create panel using a object that defines what should print and a title
     *
     * @param acorn printableObject Object that controls what should print
     * @param title Title for this printout
     */
    public PrintPreview(AcornPrintable printableObject, String title)
	                                                      throws PrinterException
    {
       super(title);
       
    
       // Set the printable object instance variable.
       msgs = LanguageText.getMessageList("acornsApplication", 137);
       target = printableObject;
       
       // Create the component to handle the print command.
       JButton printButton 
         = new JButton(msgs[0]
               , Environment.getIcon(AcornsProperties.PRINT, ICON_SIZE));
       
       // Anonymous ActionListener to respond to the print command.
       ActionListener printListener = new ActionListener()
       { 
           public void actionPerformed(ActionEvent e)
           {
              Frame root = JOptionPane.getRootFrame();
              PrinterJob printJob = PrinterJob.getPrinterJob();
              PageFormat pageFormat = printJob.defaultPage();

              target.resetDom();
              printJob.setPrintable(target, pageFormat);
	      try
	      {
                 if (printJob.printDialog())
              {
                  setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                  printJob.print();
                  setCursor( Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
              }
              dispose();
	   }
	   catch (PrinterException exception)
	   {  String message = exception.toString();
	      message = message.substring(message.indexOf(":")+1);
	      JOptionPane.showMessageDialog(root, message, msgs[2],
	      JOptionPane.PLAIN_MESSAGE);
	   }
        }   // End of actionPerformed.
       };   // End of anonymous action listener.
          
       printButton.addActionListener(printListener);
       printButton.setBorder(BorderFactory.createEtchedBorder() );    
       printButton.setAlignmentY(0.5f);
       printButton.setMargin(new Insets(4,6,4,6));
		 
       // Create the component to handle the close command.
       JButton closeButton = new JButton
            ("Close",Environment.getIcon(AcornsProperties.CLOSE, ICON_SIZE));
       
       // Create anonymous listener to respond to the close command.
       ActionListener closeListener = new ActionListener() 
       { 
           public void actionPerformed(ActionEvent e)   { dispose(); }
       };
       
       closeButton.addActionListener(closeListener);
       closeButton.setBorder(BorderFactory.createEtchedBorder() );    
       closeButton.setAlignmentY(0.5f);
       closeButton.setMargin(new Insets(4,6,4,6));
       
       // Create the component for handling the scale size of the print output.
       String[] scales = { "10 %", "25 %", "50 %", "100 %" };
       previewScale = new JComboBox<String>(scales);

       // Create panel to handle preview components.
       preview = new PreviewContainer();

       // Get size of print pages.    
       PrinterJob printJob = PrinterJob.getPrinterJob();
       PageFormat pageFormat = printJob.defaultPage();
       if (pageFormat.getHeight()==0 || pageFormat.getWidth()==0) 
       {   Frame root = JOptionPane.getRootFrame();
           JOptionPane.showMessageDialog(root, msgs[1]);
           return;
       }
       pageWidth =  (int)(pageFormat.getWidth());
       pageHeight = (int)(pageFormat.getHeight());
       
       // Scale so output will fit on the display.
       int scale = START_SCALE;
       int scaledWidth  = pageWidth*scale/100;
       int scaledHeight = pageHeight*scale/100;

       // Fill up the print preview panel with pages.
       int pageIndex = 0;
       while (true) 
       {
           BufferedImage bufferedImage= new BufferedImage
                 (pageWidth, pageHeight, BufferedImage.TYPE_INT_RGB);
           Graphics page = bufferedImage.createGraphics();
           page.setColor(Color.white);
           page.fillRect(0, 0, pageWidth, pageHeight);
           if (target.print(page, pageFormat, pageIndex)==Printable.PAGE_EXISTS)
           {
              PagePreview previewPage 
                    = new PagePreview(scaledWidth, scaledHeight, bufferedImage);
              preview.add(previewPage);
              page.dispose();
              pageIndex++;
           }
           else break;
       }
 
       // Create scroll pane with preview panel.
       final JScrollPane previewScrollPane = new JScrollPane(preview);
       previewScrollPane.getVerticalScrollBar().setUnitIncrement(50);
       // Create the anonymous listener to respond to the combo box.
       ActionListener comboBoxListener = new ActionListener() 
       { 
          public void actionPerformed(ActionEvent e) 
          { 
             // Anonymous thread class to do the scaling.
             Thread runner = new Thread() 
             {
                public @Override void run() 
                {
                   // Convert the combo selection to an integer.
                   String str = previewScale.getSelectedItem().toString();
                   if (str.endsWith("%")) str = str.substring(0, str.length()-1);
                   str = str.trim();
                   
                   int scale = 0;
                   try 
            						 { 
						                 scale = Integer.parseInt(str); 
							                if (scale<MIN_SCALE || scale>MAX_SCALE) 
							                throw new NumberFormatException();
						             }
                   catch (NumberFormatException ex) 
                   { 
                       Toolkit.getDefaultToolkit().beep();
                       return; 
                   }
                   int width  = pageWidth*scale/100;
                   int height = pageHeight*scale/100;
 
                   // Get array of components from preview container.
                   Component[] comps = preview.getComponents();
                   for (int k=0; k<comps.length; k++) 
                   {
                      // Only rescale PagePreview components.
                      if (!(comps[k] instanceof PagePreview))  continue;
                      PagePreview previewPage = (PagePreview)comps[k];
                      previewPage.setScaledSize(width, height);
                   }
                   
                   // Layout the panel with the newly scaled components.
                   preview.doLayout();
                   preview.getParent().getParent().validate();
                }
             }; // End of the anonymous thread class.
             
             runner.start();
          }  // End of actionPerformed.
       };    // End of anonymous action listener.
       
       previewScale.addActionListener(comboBoxListener);
       previewScale.setMaximumSize(previewScale.getPreferredSize());
       previewScale.setEditable(true);

       // Create the tool bar and add components to it.
       JToolBar toolBar = new JToolBar();
       toolBar.add(printButton);
       toolBar.add(closeButton);
       toolBar.addSeparator();
       toolBar.add(previewScale);

       // Configure the print preview frame.
       Container frameContainer = getContentPane();    
       frameContainer.add(toolBar, BorderLayout.NORTH);
       frameContainer.add(previewScrollPane, BorderLayout.CENTER);

       setDefaultCloseOperation(DISPOSE_ON_CLOSE);
       setIconImage
               (Environment.getIcon(AcornsProperties.ACORN, ICON_SIZE).getImage());		 
	   setSize(700, 500);
	   setPreferredSize(new Dimension(700,500));
	   setLocationRelativeTo(null);
       setVisible(true);
   }   // End of constructor.

}
  
//------------------------------------------------------------
// Class containing a series of print preview pages.  
//------------------------------------------------------------
class PreviewContainer extends JPanel
{
    private static final long serialVersionUID=1L;

    protected final int HORIZONTAL_GAP = 16;
    protected final int VERTICAL_GAP   = 10;

  	 //------------------------------------------------------------
    // Compute preferred size of this component.
	 //------------------------------------------------------------
    public @Override Dimension getPreferredSize() 
    {
       // Get component count; return if it is zero.
       int componentCount = getComponentCount();
       if (componentCount == 0)
           return new Dimension(HORIZONTAL_GAP, VERTICAL_GAP);
      
       // Get size of the first print preview component.
       Component component = getComponent(0);
       Dimension componentSize = component.getPreferredSize();
       int width  = componentSize.width;
       int height = componentSize.height;

       Dimension grid = getRowsAndColumns();           
       Insets insets = getInsets();
		 
		 int totalWidth  = grid.width  * (width + HORIZONTAL_GAP) + HORIZONTAL_GAP;
		 int totalHeight = grid.height * (height + VERTICAL_GAP) + VERTICAL_GAP;
       return new Dimension(totalWidth + insets.left + insets.right
		               , totalHeight + insets.top + insets.bottom);
	 }
	 
	 public @Override Dimension getMaximumSize() 
         { return getPreferredSize(); }
	 public @Override Dimension getMinimumSize() 
         { return getPreferredSize(); }
    
	 //------------------------------------------------------------
    // Method to compute the number of rows and columns.
	 //------------------------------------------------------------
    public Dimension getRowsAndColumns()
    {
	    int componentCount = getComponentCount();
		 
       // Get size of the first print preview component.
       Component component = getComponent(0);
       Dimension componentSize = component.getPreferredSize();
       int width  = componentSize.width;
           
       // Get size of the panel holding this component.          
       Dimension parentSize = getParent().getSize();
       
       // Compute how many components can show on each row, and the number of rows.
       int columns = Math.max
                        ((parentSize.width-HORIZONTAL_GAP)/(width+HORIZONTAL_GAP), 1);
       int rows    = componentCount/columns;
       if (rows*columns < componentCount)  rows++;
       return new Dimension(columns, rows);
    }

	 //------------------------------------------------------------
  // Set position of each component in the panel.
	 //------------------------------------------------------------
    public @Override void doLayout() 
    {
        int componentCount = getComponentCount();
        if (componentCount == 0)   return;
        
        // Compute position of starting component.
        Insets insets = getInsets();
        int x = insets.left + HORIZONTAL_GAP;
        int y = insets.top + VERTICAL_GAP;
		  
        // Get the size of each component.
        Component component = getComponent(0);
        Dimension componentSize = component.getPreferredSize();
        int width  = componentSize.width;
        int height = componentSize.height;

        Dimension grid = getRowsAndColumns();

        // Now set the location of each component.
        int index = 0;
        for (int k = 0; k<grid.height; k++) 
        {
            for (int m = 0; m<grid.width; m++) 
            {
                if (index >= componentCount)  {return;}       
                // Get next component and set the bounds.
                component = getComponent(index++);
                component.setBounds(x, y, width, height);
                x += width + HORIZONTAL_GAP;
            }
            y += height + VERTICAL_GAP;
            x = insets.left + HORIZONTAL_GAP;
        }
    }   // End of doLayout.
}

/** Panel to hold an image of the appropriate size for print preview commands
 */
class PagePreview extends JPanel
{
    private static final long serialVersionUID=1L;

    protected int   pageWidth;
    protected int   pageHeight;
    protected Image sourceImage;
    protected Image panelImage;

    /** Constructor to create display panel holding image
     *
     * @param pageWidth Width of the panel
     * @param pageHeight Height of the panel
     * @param sourceImage image to insert into the panel
     */
    public PagePreview(int pageWidth, int pageHeight, Image sourceImage) 
    {
       this.pageWidth   = pageWidth;
       this.pageHeight  = pageHeight;
       this.sourceImage = sourceImage;
       this.panelImage  = sourceImage.getScaledInstance
                             (pageWidth, pageHeight, Image.SCALE_SMOOTH);
       
       panelImage.flush();
       setBackground(Color.white);
       setBorder(new MatteBorder(1, 1, 2, 2, Color.black));
    }

	   /** Set the scaled size of the image
     *
     * @param width width of scaled image
     * @param height of scaled image
     */
    public void setScaledSize(int width, int height) 
    {
        pageWidth  = width;
        pageHeight = height;
        panelImage = sourceImage.getScaledInstance
                        (pageWidth, pageHeight, Image.SCALE_SMOOTH);
        repaint();
    }

    /** Get preferred size for thhis panel
     */
    public @Override Dimension getPreferredSize() 
    {
       Insets insets = getInsets();
       return new Dimension
            (pageWidth+insets.left+insets.right, pageHeight+insets.top+insets.bottom);
    }
	 
    /** Get minimum size for thhis panel
     */
    public @Override Dimension getMinimumSize() { return getPreferredSize(); }
    /** Get maximum size for thhis panel
     */
    public @Override Dimension getMaximumSize() { return getPreferredSize(); }

  /** Draw the panel with an image and bounding rectangle
   *
   * @param page Graphics object to draw into
   */
    public @Override void paint(Graphics page) 
    {
       page.setColor(getBackground());
       page.fillRect(0, 0, getWidth(), getHeight());
       page.drawImage(panelImage, 0, 0, this);
       paintBorder(page);
    }
}   // End of PagePreview class.
