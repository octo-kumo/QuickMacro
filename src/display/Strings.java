package display;

import main.Main;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

public abstract class Strings {
    public String LANG, MOUSE, KEYS, FILE, SAVE, OPEN, HOTKEY, RECORD, STOP, PLAY, CHANGE_HOTKEY_HINT, SET_TIME_TO, ABOUT, ABOUT_TITLE, CHANGE_LANG_WARN, WARNING;
    public static final Strings ZH = new Strings() {{
        LANG = "語言";
        MOUSE = "鼠標";
        KEYS = "鍵盤";
        FILE = "文件";
        SAVE = "保存...";
        OPEN = "打開...";
        HOTKEY = "快捷鍵";
        RECORD = "錄製";
        STOP = "停止";
        PLAY = "啟動";
        CHANGE_HOTKEY_HINT = "按住SHIFT換鍵";
        SET_TIME_TO = "將時間設為：";
        ABOUT = "速成脚本\n作者： @云";
        ABOUT_TITLE = "關於";
        CHANGE_LANG_WARN = "是否立即重啟？";
    }};

    public static final Strings EN = new Strings() {{
        LANG = "Language";
        MOUSE = "Mouse";
        KEYS = "Keys";
        FILE = "File";
        SAVE = "Save...";
        OPEN = "Open...";
        HOTKEY = "Hotkey";
        RECORD = "Record";
        STOP = "Stop";
        PLAY = "Play";
        CHANGE_HOTKEY_HINT = "Use 'Shift' to change";
        SET_TIME_TO = "Set time to:";
        ABOUT = "Quick Macro\nAuthor： @Kumo";
        ABOUT_TITLE = "About";
        CHANGE_LANG_WARN = "Restart now?";
    }};

    public static Strings get() {
        return Settings.preferences.get("lang", "en").equals("en") ? EN : ZH;
    }

    public static void changeTo(String lang) {
        Settings.preferences.put("lang", lang);
        int i = JOptionPane.showConfirmDialog(null, get().CHANGE_LANG_WARN, get().WARNING, JOptionPane.OK_CANCEL_OPTION);
        if (i == JOptionPane.OK_OPTION) {
            Main.getInstance().dispose();
            try {
                restartApplication();
                System.exit(0);
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void restartApplication() throws URISyntaxException, IOException {
        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final File currentJar = new File(Strings.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        if (!currentJar.getName().endsWith(".jar")) return;

        final ArrayList<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-jar");
        command.add(currentJar.getPath());

        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.start();
    }
}
