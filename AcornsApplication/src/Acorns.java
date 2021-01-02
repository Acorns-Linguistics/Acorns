import java.awt.Frame;
import java.io.File;
import java.net.URL;

import javax.swing.JOptionPane;

import org.acorns.Environment;
import org.acorns.audio.SoundDefaults;
import org.acorns.language.LanguageText;

import acornsapplication.Acorn;
import acornsapplication.AcornMenu;
import acornsapplication.AppEnv;
import acornsapplication.FileOpen;
import acornsapplication.JarLoader;

public class Acorns 
{
    private static String os = System.getProperty("os.name").toLowerCase();
	
	public static void main(String[] args)
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");

        if (args.length!=0 && args[0]!=null && !args[0].endsWith(".lnx"))
        {
        	Frame frame = JOptionPane.getRootFrame();
            JOptionPane.showMessageDialog(frame,
            LanguageText.getMessage("acornsApplication", 143, args[0]));
            System.exit(1);
        }
  
        String libraryFolder = "Acorns_lib";

        // Tell Java where the JavaFX media files reside
	    String addition = System.getProperty("java.library.path") 
			  + File.pathSeparator 
			  + System.getProperty("user.dir")
			  + File.separator + libraryFolder;
	  
	    System.setProperty("java.library.path", addition);
		
	    SoundDefaults.setSandboxKey("org.acorns.acorns");
		System.setProperty("java.library.path", addition);
		  
	    AppEnv.readStatus();
        helpSets();
        
		if (SoundDefaults.isSandboxed())
		{
			String libName = "SecurityScopedBookmarkLibrary";
			System.loadLibrary(libName);
			  
			Environment.getHelpSet();
			boolean resetPaths = SoundDefaults.setBookmarkFolder();
		    if (resetPaths)
		    {
		    	  String data = SoundDefaults.getDataFolder();
		    	  AppEnv.resetPaths(data);
		    }
		}

       
        // Try to use jar location from previous run
        URL url = new Acorns().getClass().getResource("/data/");
        new JarLoader(url);

        try
        {
        	new Acorn(null); // Start the application
            fileAssociations();
        	if (os.indexOf("mac") >= 0) 
        	{
        		new JavaAwtDesktop();
        		System.setProperty("apple.laf.useScreenMenuBar", "true");
        		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "ACORNS");
        	}
        	
            if (args.length>0) 
            { 
                try
                {  
                   String status =  FileOpen.openFile(new File(args[0])); 
                   AppEnv.setText(status);         // Status of the open
                   AcornMenu.setEnable();          // Activate the icons  
                }
                catch (Exception e) 
                { 
                	AppEnv.setText("Main: " + args[0] + ":" + e.toString());  
                }

            }
        }
        catch (Throwable t)
        { 
        	Frame frame = JOptionPane.getRootFrame();
        	JOptionPane.showMessageDialog(frame, "Main: " + t.toString());
        }
 

	}
	
	public static void helpSets()
	{
        javax.help.HelpSet helpSet = Environment.getHelpSet();
        new javax.help.DefaultHelpBroker(helpSet);
        new LanguageText(Environment.getHelpSet());
	}

    /** Check file associations */
    private static void fileAssociations() throws Throwable
    {
        if (os.toLowerCase().indexOf("win")>=0)
        {
            String separator = System.getProperty("file.separator");
            String path = Environment.getWorkingDirectory();
            String file = path + separator + "Acorns.exe";
            File   exe  = new File(file);
            if (exe.exists())  
            {
                try
                {
                   String command = "cmd /c assoc .lnx=acorns";
                   Process p = Runtime.getRuntime().exec(command);
                   p.waitFor();
                    
                   command = "cmd /c ftype acorns=\"" + file + "\" \"%1\"";
                   p = Runtime.getRuntime().exec(command);
                   p.waitFor();
                   
                }  catch (Exception e) {}
            }   // End of if executable exists
        }       // End of if window operating system          
    }           // End of file associations    

}
