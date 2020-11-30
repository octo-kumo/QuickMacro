package main;

import com.bulenkov.darcula.DarculaLaf;
import display.Editor;
import display.Settings;
import display.Strings;
import org.jnativehook.GlobalScreen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JFrame {

    private static Main instance;
    private final Editor editor;

    public static Main getInstance() {
        return instance;
    }

    public Main() throws AWTException {
        instance = this;
        editor = new Editor();
        setContentPane(editor);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setJMenuBar(editor.menuBar);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.out.println("Exiting");
                editor.exit();
            }
        });
    }

    public static void main(String... args) throws AWTException {
        String mrjVersion = System.getProperty("mrj.version");
        boolean isMacOs = mrjVersion != null;
        if (isMacOs) {
            com.apple.eawt.Application.getApplication().setQuitStrategy(com.apple.eawt.QuitStrategy.CLOSE_ALL_WINDOWS);
        }

        JComponent.setDefaultLocale(Settings.preferences.get("lang", "en").equals("en") ? Locale.ENGLISH : Locale.TRADITIONAL_CHINESE);
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);
        try {
            UIManager.setLookAndFeel(new DarculaLaf());
        } catch (UnsupportedLookAndFeelException ignored) {
        }

        Main main = new Main();
        if (isMacOs) {
            com.apple.eawt.Application.getApplication().setOpenFileHandler(openFilesEvent -> main.editor.openFile(openFilesEvent.getFiles().get(0)));
            com.apple.eawt.Application.getApplication().setAboutHandler(aboutEvent -> JOptionPane.showMessageDialog(null, Strings.get().ABOUT, Strings.get().ABOUT_TITLE, JOptionPane.INFORMATION_MESSAGE));
        }
        main.setVisible(true);
        main.pack();
    }
}
