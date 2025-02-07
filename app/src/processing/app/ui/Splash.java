package processing.app.ui;

import processing.app.Platform;
import processing.app.Settings;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import javax.swing.JComponent;
import javax.swing.JFrame;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;


/**
 * Show a splash screen window. Loosely based on SplashWindow.java from
 * Werner Randelshofer, but rewritten to use Swing because the java.awt
 * version doesn't render properly on Windows.
 */
public class Splash extends JFrame {
  static private Splash instance;
  private final Image image;


  private Splash(File imageFile, boolean hidpi) {
    // Putting this inside try/catch because it's not essential,
    // and it's definitely not essential enough to prevent startup.
    try {
      // change default java window icon to processing icon
      processing.app.ui.Toolkit.setIcon(this);
    } catch (Exception e) {
      // ignored
    }
    
    this.image =
      Toolkit.getDefaultToolkit().createImage(imageFile.getAbsolutePath());

    MediaTracker tracker = new MediaTracker(this);
    tracker.addImage(image,0);
    try {
      tracker.waitForID(0);
    } catch (InterruptedException ignored) { }

    if (tracker.isErrorID(0)) {
      // Abort on failure
      setSize(0,0);
      System.err.println("Warning: SplashWindow couldn't load splash image.");
      synchronized (this) {
        notifyAll();
      }
    } else {
      final int imgWidth = image.getWidth(this);
      final int imgHeight = image.getHeight(this);
      final int imgScale = hidpi ? 2 : 1;

      JComponent comp = new JComponent() {
        final int wide = imgWidth / imgScale;
        final int high = imgHeight / imgScale;

        public void paintComponent(Graphics g) {
          g.drawImage(image, 0, 0, wide, high, this);
        }

        public Dimension getPreferredSize() {
          return new Dimension(wide, high);
        }
      };
      comp.setSize(imgWidth, imgHeight);
      setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
      getContentPane().add(comp);
      setUndecorated(true);  // before pack()
      pack();
      setLocationRelativeTo(null);  // center on screen
    }
  }


  /** Open a splash window using the specified image. */
  static void showSplash(File imageFile, boolean hidpi) {
    if (instance == null) {
      instance = new Splash(imageFile, hidpi);
      instance.setVisible(true);
    }
  }


  /** Closes the splash window when finished. */
  static void disposeSplash() {
    if (instance != null) {
      //instance.getOwner().dispose();
      instance.dispose();
      instance = null;
    }
  }


  /**
   * Invokes the main method of the provided class name.
   * @param args the command line arguments
   */
  static void invokeMain(String className, String[] args) {
    try {
      Class.forName(className)
        .getMethod("main", String[].class)
        .invoke(null, new Object[] { args });

    } catch (Exception e) {
      throw new InternalError("Failed to invoke main method", e);
    }
  }


//  /**
//   * Load the optional properties.txt file from the 'lib' sub-folder
//   * that can be used to pass entries to System.properties.
//   */
//  static private void initProperties() {
//    try {
//      File propsFile = Platform.getContentFile("properties.txt");
//      if (propsFile != null && propsFile.exists()) {
//        Settings props = new Settings(propsFile);
//        for (Map.Entry<String, String> entry : props.getMap().entrySet()) {
//          System.setProperty(entry.getKey(), entry.getValue());
//        }
//      }
//    } catch (Exception e) {
//      // No crying over spilt milk, but...
//      e.printStackTrace();
//    }
//  }


  static public boolean getDisableHiDPI() {
    File propsFile = Platform.getContentFile("disable_hidpi");
    if (propsFile != null && propsFile.exists()) {
      return true;
    }
    return false;
  }


  static public void setDisableHiDPI(boolean disabled) {
    try {
      File propsFile = Platform.getContentFile("disable_hidpi");
      if (propsFile != null) {
        if (disabled) {
          new FileOutputStream(propsFile).close();
        } else {
          boolean success = propsFile.delete();
          if (!success) {
            System.err.println("Could not delete disable_hidpi");
          }
        }
      }
    } catch (Exception e) {
      // No crying over spilt milk, but...
      e.printStackTrace();
    }
  }


  static public void main(String[] args) {
    // Has to be done before AWT is initialized, so the hack lives here
    if (getDisableHiDPI()) {
      System.setProperty("sun.java2d.uiScale.enabled", "false");
    }
    try {
      final boolean hidpi = processing.app.ui.Toolkit.highResImages();
      final String filename = "lib/about-" + (hidpi ? 2 : 1) + "x.png";
      File splashFile = Platform.getContentFile(filename);
      showSplash(splashFile, hidpi);
      invokeMain("processing.app.Base", args);
      disposeSplash();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
