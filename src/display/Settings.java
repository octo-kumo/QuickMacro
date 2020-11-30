package display;

import com.google.gson.Gson;
import controllers.Recorder;
import main.Main;
import org.jnativehook.keyboard.NativeKeyEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.Preferences;

public class Settings {
    public static final Gson gson = new Gson();
    public static final Preferences preferences = Preferences.userNodeForPackage(Settings.class);
    public static final Hotkey RECORD_KEY = new Hotkey("record_key", KeyEvent.VK_R, KeyEvent.SHIFT_DOWN_MASK);
    public static final Hotkey PLAY_KEY = new Hotkey("play_key", KeyEvent.VK_P, KeyEvent.SHIFT_DOWN_MASK);
    public static final Hotkey STOP_KEY = new Hotkey("stop_key", KeyEvent.VK_ESCAPE, 0);

    public static void save() {
        preferences.put("record_key", gson.toJson(RECORD_KEY));
        preferences.put("play_key", gson.toJson(PLAY_KEY));
        preferences.put("stop_key", gson.toJson(STOP_KEY));
    }

    public static class Hotkey {
        private final String key;
        public int keycode;
        public int modifiers;

        public Hotkey(String key, int keycode, int modifiers) {
            this.key = key;
            this.keycode = preferences.getInt(key + "_keycode", keycode);
            this.modifiers = preferences.getInt(key + "_modifiers", modifiers);
        }

        public boolean accepts(NativeKeyEvent event) {
            return Recorder.translateKey(event.getKeyCode()) == keycode && (modifiers == 0 || Recorder.translateModifier(event.getModifiers()) == modifiers);
        }

        public void change(int keycode, int modifiers) {
            preferences.putInt(key + "_keycode", this.keycode = keycode);
            preferences.putInt(key + "_modifiers", this.modifiers = modifiers);
        }

        public String name() {
            return KeyEvent.getKeyModifiersText(modifiers) + "+" + KeyEvent.getKeyText(keycode);
        }
    }

    public static class HotkeyView extends JMenuItem implements ActionListener {
        private final String title;
        private final Hotkey hotkey;
        private final ActionListener action;

        public HotkeyView(String title, Hotkey hotkey, ActionListener action) {
            this.title = title;
            this.hotkey = hotkey;
            this.action = action;
            addActionListener(this);
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                int[] data = {0, 0};
                JDialog dialog = new JDialog(Main.getInstance(), "换快捷键", true);
                dialog.setLayout(new FlowLayout());
                dialog.add(new JButton("点点我uwu") {{
                    addActionListener(e -> setText("点点键"));
                    addKeyListener(new KeyAdapter() {
                        public void keyPressed(KeyEvent e) {
                            data[0] = e.getKeyCode();
                            data[1] = e.getModifiers();
                            String modifiersText = KeyEvent.getKeyModifiersText(data[1]);
                            setText(title + " " + (modifiersText.length() == 0 ? "" : modifiersText + "+") + KeyEvent.getKeyText(data[0]));
                        }
                    });
                }});
                dialog.add(new JButton("OK") {{
                    addActionListener(e -> {
                        hotkey.change(data[0], data[1]);
                        HotkeyView.this.update();
                        dialog.dispose();
                    });
                }});
                dialog.add(new JButton("取消") {{
                    addActionListener(e -> dialog.dispose());
                }});
                dialog.pack();
                dialog.setVisible(true);
            } else action.actionPerformed(e);
        }

        public void update() {
            String modifiersText = KeyEvent.getKeyModifiersText(hotkey.modifiers);
            setText(title + " " + (modifiersText.length() == 0 ? "" : modifiersText + "+") + KeyEvent.getKeyText(hotkey.keycode));
        }
    }
}
