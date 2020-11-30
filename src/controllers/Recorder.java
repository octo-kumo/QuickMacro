package controllers;


import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.sun.glass.events.MouseEvent;
import display.Editor;
import display.Settings;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;
import org.jnativehook.mouse.NativeMouseMotionListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class Recorder implements NativeKeyListener, NativeMouseListener, NativeMouseMotionListener {

    private final Robot robot;
    private final Timer timer;
    public final ArrayList<MousePos> mousePositions;
    public final ArrayList<Press> mousePresses;
    public final ArrayList<Press> keyPresses;
    public final ArrayList<ScreenState> screenStates;

    private final ArrayList<Press> activeKeys;
    private final ArrayList<Press> activeButtons;

    private long startTime;

    private int mouseIndex;
    private int mousePressIndex;
    private int keyIndex;

    public long currentTime;
    private final Editor editor;

    public enum RecorderState {IDLE, RECORDING, PLAYING}

    private RecorderState state = RecorderState.IDLE;

    private int frameCounter = 0;
    private Rectangle capture;

    public Recorder(Editor editor) throws AWTException {
        this.editor = editor;
        mousePositions = new ArrayList<>();
        keyPresses = new ArrayList<>();
        mousePresses = new ArrayList<>();
        screenStates = new ArrayList<>();

        activeKeys = new ArrayList<>();
        activeButtons = new ArrayList<>();

        timer = new Timer(8, e -> update());
        robot = new Robot();
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            System.exit(1);
        }
        GlobalScreen.addNativeMouseMotionListener(this);
        GlobalScreen.addNativeKeyListener(this);
        GlobalScreen.addNativeMouseListener(this);
        if (!new File("./cache").exists()) new File("./cache").mkdir();
    }

    public void record() {
        if (state != RecorderState.IDLE) return;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        capture = new Rectangle(0, 0, screenSize.width, screenSize.height);
        state = RecorderState.RECORDING;
        mouseIndex = 0;
        mousePressIndex = 0;
        keyIndex = 0;
        startTime = System.currentTimeMillis();
        mousePositions.clear();
        keyPresses.clear();
        mousePresses.clear();
        timer.start();
    }

    private void update() {
        currentTime = System.currentTimeMillis() - startTime;
        if (state == RecorderState.PLAYING) {
            execute();
            editor.playbackCallback();
        }
        if (state == RecorderState.RECORDING) {
            if (frameCounter % 5 == 0)
                screenStates.add(new ScreenState(currentTime, resize(robot.createScreenCapture(capture))));
            frameCounter++;
        }
    }

    public static BufferedImage resize(BufferedImage img) {
        Image tmp = img.getScaledInstance(img.getWidth() / 2, img.getHeight() / 2, Image.SCALE_FAST);
        BufferedImage dimg = new BufferedImage(img.getWidth() / 2, img.getHeight() / 2, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return dimg;
    }

    public void execute() {
        while (true) { // purely mouse genius
            if (mouseIndex >= mousePositions.size() && keyIndex >= keyPresses.size() && mousePressIndex >= mousePresses.size() && activeKeys.size() == 0 && activeButtons.size() == 0) { // run out of stuff
                state = RecorderState.IDLE;
                break;
            }
            boolean stop = true;
            if (mouseIndex < mousePositions.size()) {
                MousePos mousePos = mousePositions.get(mouseIndex);
                if (mousePos.time < currentTime) {
                    robot.mouseMove(mousePos.x, mousePos.y);
                    mouseIndex++;
                    stop = false;
                }
            }

            if (keyIndex < keyPresses.size()) {
                Press press = keyPresses.get(keyIndex);
                if (press.time < currentTime) {
                    robot.keyPress(press.code);
                    activeKeys.add(press);
                    keyIndex++;
                    stop = false;
                }
            }
            for (int i = activeKeys.size() - 1; i >= 0; i--) {
                Press press = activeKeys.get(i);
                if (press.time + press.duration < currentTime) {
                    robot.keyRelease(press.code);
                    activeKeys.remove(i);
                }
            }

            if (mousePressIndex < mousePresses.size()) {
                Press press = mousePresses.get(mousePressIndex);
                if (press.time < currentTime) {
                    robot.mousePress(press.code);
                    activeButtons.add(press);
                    mousePressIndex++;
                    stop = false;
                }
            }
            for (int i = activeButtons.size() - 1; i >= 0; i--) {
                Press press = activeButtons.get(i);
                if (press.time + press.duration < currentTime) {
                    robot.mouseRelease(press.code);
                    activeButtons.remove(i);
                }
            }
            if (stop) break;
        }
    }

    public void playBack() {
        if (state != RecorderState.IDLE) return;
        state = RecorderState.PLAYING;
        mouseIndex = 0;
        mousePressIndex = 0;
        keyIndex = 0;
        startTime = System.currentTimeMillis();
        timer.start();
        activeKeys.clear();
        activeButtons.clear();
    }

    public void stop() {
        state = RecorderState.IDLE;
        timer.stop();
    }

    public void exit() {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }
    }

    public void nativeMouseDragged(NativeMouseEvent nativeMouseEvent) {
        if (state == RecorderState.RECORDING) {
            currentTime = System.currentTimeMillis() - startTime;
            mousePositions.add(new MousePos(nativeMouseEvent.getPoint(), currentTime));
        }
    }

    public void nativeMouseMoved(NativeMouseEvent nativeMouseEvent) {
        if (state == RecorderState.RECORDING && (mousePositions.size() == 0 || mousePositions.get(mousePositions.size() - 1).dist2(nativeMouseEvent.getPoint()) > 2)) {
            currentTime = System.currentTimeMillis() - startTime;
            mousePositions.add(new MousePos(nativeMouseEvent.getPoint(), currentTime));
        }
    }

    public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
        if (state == RecorderState.RECORDING) {
            currentTime = System.currentTimeMillis() - startTime;
            keyPresses.add(new Press(translateKey(nativeKeyEvent.getKeyCode()), currentTime));
        }
        if (Settings.PLAY_KEY.accepts(nativeKeyEvent) && mousePositions.size() > 0) playBack();
        else if (Settings.RECORD_KEY.accepts(nativeKeyEvent)) record();

        if (Settings.STOP_KEY.accepts(nativeKeyEvent)) {
            stop();
            editor.update();
        }
    }

    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
        if (state == RecorderState.RECORDING) {
            currentTime = System.currentTimeMillis() - startTime;
            int key = translateKey(nativeKeyEvent.getKeyCode());
            Optional<Press> reduce = keyPresses.stream().filter(p -> p.code == key).reduce((first, second) -> second);
            reduce.ifPresent(press -> press.duration = (int) (currentTime - press.time));
        }
    }

    public void nativeMousePressed(NativeMouseEvent nativeMouseEvent) {
        if (state == RecorderState.RECORDING) {
            currentTime = System.currentTimeMillis() - startTime;
            mousePresses.add(new Press(translateButton(nativeMouseEvent.getButton()), currentTime));
        }
    }

    public void nativeMouseReleased(NativeMouseEvent nativeMouseEvent) {
        if (state == RecorderState.RECORDING) {
            currentTime = System.currentTimeMillis() - startTime;
            int button = translateButton(nativeMouseEvent.getButton());
            Optional<Press> reduce = mousePresses.stream().filter(p -> p.code == button).reduce((first, second) -> second);
            reduce.ifPresent(press -> press.duration = (int) (currentTime - press.time));
        }
    }

    public static class TimedObject {
        public long time;
    }

    public static class ScreenState extends TimedObject {
        public long time;

        public ScreenState(long time, BufferedImage state) {
            this.time = time;
            try {
                ImageIO.write(state, "png", new File("./cache/" + time + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public BufferedImage getImage() {
            try {
                return ImageIO.read(new File("./cache/" + time + ".png"));
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static class MousePos extends TimedObject {
        public int x, y;

        public MousePos(Point point, long time) {
            this.x = point.x;
            this.y = point.y;
            this.time = time;
        }

        public int dist2(Point other) {
            return (other.x - x) * (other.x - x) + (other.y - y) * (other.y - y);
        }
    }

    public static class Press extends TimedObject {
        public int code;
        public int duration;

        public Press(int code, long start) {
            this.code = code;
            this.time = start;
        }
    }

    public void saveToFile() {
        Gson gson = new Gson();
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                RecordedData object = new RecordedData(mousePositions, mousePresses, keyPresses);
                FileWriter writer = new FileWriter(file);
                gson.toJson(object, RecordedData.class, writer);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void openFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            openFromFile(file);
        }
    }

    private static class RecordedData {
        public ArrayList<MousePos> mousePositions;
        public ArrayList<Press> mousePresses;
        public ArrayList<Press> keyPresses;

        public RecordedData() {
        }

        public RecordedData(ArrayList<MousePos> mousePositions, ArrayList<Press> mousePresses, ArrayList<Press> keyPresses) {
            this.mousePositions = mousePositions;
            this.mousePresses = mousePresses;
            this.keyPresses = keyPresses;
        }
    }

    public void openFromFile(File file) {
        Gson gson = new Gson();
        try {
            RecordedData object = gson.fromJson(new JsonReader(new FileReader(file)), RecordedData.class);
            this.mousePositions.clear();
            this.mousePresses.clear();
            this.keyPresses.clear();
            this.mousePositions.addAll(object.mousePositions);
            this.mousePresses.addAll(object.mousePresses);
            this.keyPresses.addAll(object.keyPresses);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {
    }

    public void nativeMouseClicked(NativeMouseEvent nativeMouseEvent) {
    }

    public static int translateKey(int oldCode) {
        int keyCode = KeyEvent.VK_UNDEFINED;
        switch (oldCode) {
            case NativeKeyEvent.VC_ESCAPE:
                keyCode = KeyEvent.VK_ESCAPE;
                break;
            case NativeKeyEvent.VC_F1:
                keyCode = KeyEvent.VK_F1;
                break;
            case NativeKeyEvent.VC_F2:
                keyCode = KeyEvent.VK_F2;
                break;
            case NativeKeyEvent.VC_F3:
                keyCode = KeyEvent.VK_F3;
                break;

            case NativeKeyEvent.VC_F4:
                keyCode = KeyEvent.VK_F4;
                break;

            case NativeKeyEvent.VC_F5:
                keyCode = KeyEvent.VK_F5;
                break;

            case NativeKeyEvent.VC_F6:
                keyCode = KeyEvent.VK_F6;
                break;

            case NativeKeyEvent.VC_F7:
                keyCode = KeyEvent.VK_F7;
                break;

            case NativeKeyEvent.VC_F8:
                keyCode = KeyEvent.VK_F8;
                break;

            case NativeKeyEvent.VC_F9:
                keyCode = KeyEvent.VK_F9;
                break;

            case NativeKeyEvent.VC_F10:
                keyCode = KeyEvent.VK_F10;
                break;

            case NativeKeyEvent.VC_F11:
                keyCode = KeyEvent.VK_F11;
                break;

            case NativeKeyEvent.VC_F12:
                keyCode = KeyEvent.VK_F12;
                break;

            case NativeKeyEvent.VC_F13:
                keyCode = KeyEvent.VK_F13;
                break;

            case NativeKeyEvent.VC_F14:
                keyCode = KeyEvent.VK_F14;
                break;

            case NativeKeyEvent.VC_F15:
                keyCode = KeyEvent.VK_F15;
                break;

            case NativeKeyEvent.VC_F16:
                keyCode = KeyEvent.VK_F16;
                break;

            case NativeKeyEvent.VC_F17:
                keyCode = KeyEvent.VK_F17;
                break;

            case NativeKeyEvent.VC_F18:
                keyCode = KeyEvent.VK_F18;
                break;

            case NativeKeyEvent.VC_F19:
                keyCode = KeyEvent.VK_F19;
                break;
            case NativeKeyEvent.VC_F20:
                keyCode = KeyEvent.VK_F20;
                break;

            case NativeKeyEvent.VC_F21:
                keyCode = KeyEvent.VK_F21;
                break;

            case NativeKeyEvent.VC_F22:
                keyCode = KeyEvent.VK_F22;
                break;

            case NativeKeyEvent.VC_F23:
                keyCode = KeyEvent.VK_F23;
                break;

            case NativeKeyEvent.VC_F24:
                keyCode = KeyEvent.VK_F24;
                break;
            // End Function Keys


            // Begin Alphanumeric Zone
            case NativeKeyEvent.VC_BACKQUOTE:
                keyCode = KeyEvent.VK_BACK_QUOTE;
                break;

            case NativeKeyEvent.VC_1:
                keyCode = KeyEvent.VK_1;
                break;

            case NativeKeyEvent.VC_2:
                keyCode = KeyEvent.VK_2;
                break;

            case NativeKeyEvent.VC_3:
                keyCode = KeyEvent.VK_3;
                break;

            case NativeKeyEvent.VC_4:
                keyCode = KeyEvent.VK_4;
                break;

            case NativeKeyEvent.VC_5:
                keyCode = KeyEvent.VK_5;
                break;

            case NativeKeyEvent.VC_6:
                keyCode = KeyEvent.VK_6;
                break;

            case NativeKeyEvent.VC_7:
                keyCode = KeyEvent.VK_7;
                break;

            case NativeKeyEvent.VC_8:
                keyCode = KeyEvent.VK_8;
                break;

            case NativeKeyEvent.VC_9:
                keyCode = KeyEvent.VK_9;
                break;

            case NativeKeyEvent.VC_0:
                keyCode = KeyEvent.VK_0;
                break;


            case NativeKeyEvent.VC_MINUS:
                keyCode = KeyEvent.VK_MINUS;
                break;

            case NativeKeyEvent.VC_EQUALS:
                keyCode = KeyEvent.VK_EQUALS;
                break;

            case NativeKeyEvent.VC_BACKSPACE:
                keyCode = KeyEvent.VK_BACK_SPACE;
                break;


            case NativeKeyEvent.VC_TAB:
                keyCode = KeyEvent.VK_TAB;
                break;

            case NativeKeyEvent.VC_CAPS_LOCK:
                keyCode = KeyEvent.VK_CAPS_LOCK;
                break;


            case NativeKeyEvent.VC_A:
                keyCode = KeyEvent.VK_A;
                break;

            case NativeKeyEvent.VC_B:
                keyCode = KeyEvent.VK_B;
                break;

            case NativeKeyEvent.VC_C:
                keyCode = KeyEvent.VK_C;
                break;

            case NativeKeyEvent.VC_D:
                keyCode = KeyEvent.VK_D;
                break;

            case NativeKeyEvent.VC_E:
                keyCode = KeyEvent.VK_E;
                break;

            case NativeKeyEvent.VC_F:
                keyCode = KeyEvent.VK_F;
                break;

            case NativeKeyEvent.VC_G:
                keyCode = KeyEvent.VK_G;
                break;

            case NativeKeyEvent.VC_H:
                keyCode = KeyEvent.VK_H;
                break;

            case NativeKeyEvent.VC_I:
                keyCode = KeyEvent.VK_I;
                break;

            case NativeKeyEvent.VC_J:
                keyCode = KeyEvent.VK_J;
                break;

            case NativeKeyEvent.VC_K:
                keyCode = KeyEvent.VK_K;
                break;

            case NativeKeyEvent.VC_L:
                keyCode = KeyEvent.VK_L;
                break;

            case NativeKeyEvent.VC_M:
                keyCode = KeyEvent.VK_M;
                break;

            case NativeKeyEvent.VC_N:
                keyCode = KeyEvent.VK_N;
                break;

            case NativeKeyEvent.VC_O:
                keyCode = KeyEvent.VK_O;
                break;

            case NativeKeyEvent.VC_P:
                keyCode = KeyEvent.VK_P;
                break;

            case NativeKeyEvent.VC_Q:
                keyCode = KeyEvent.VK_Q;
                break;

            case NativeKeyEvent.VC_R:
                keyCode = KeyEvent.VK_R;
                break;

            case NativeKeyEvent.VC_S:
                keyCode = KeyEvent.VK_S;
                break;

            case NativeKeyEvent.VC_T:
                keyCode = KeyEvent.VK_T;
                break;

            case NativeKeyEvent.VC_U:
                keyCode = KeyEvent.VK_U;
                break;

            case NativeKeyEvent.VC_V:
                keyCode = KeyEvent.VK_V;
                break;

            case NativeKeyEvent.VC_W:
                keyCode = KeyEvent.VK_W;
                break;

            case NativeKeyEvent.VC_X:
                keyCode = KeyEvent.VK_X;
                break;

            case NativeKeyEvent.VC_Y:
                keyCode = KeyEvent.VK_Y;
                break;

            case NativeKeyEvent.VC_Z:
                keyCode = KeyEvent.VK_Z;
                break;


            case NativeKeyEvent.VC_OPEN_BRACKET:
                keyCode = KeyEvent.VK_OPEN_BRACKET;
                break;

            case NativeKeyEvent.VC_CLOSE_BRACKET:
                keyCode = KeyEvent.VK_CLOSE_BRACKET;
                break;

            case NativeKeyEvent.VC_BACK_SLASH:
                keyCode = KeyEvent.VK_BACK_SLASH;
                break;


            case NativeKeyEvent.VC_SEMICOLON:
                keyCode = KeyEvent.VK_SEMICOLON;
                break;

            case NativeKeyEvent.VC_QUOTE:
                keyCode = KeyEvent.VK_QUOTE;
                break;

            case NativeKeyEvent.VC_ENTER:
                keyCode = KeyEvent.VK_ENTER;
                break;


            case NativeKeyEvent.VC_COMMA:
                keyCode = KeyEvent.VK_COMMA;
                break;

            case NativeKeyEvent.VC_PERIOD:
                keyCode = KeyEvent.VK_PERIOD;
                break;

            case NativeKeyEvent.VC_SLASH:
                keyCode = KeyEvent.VK_SLASH;
                break;

            case NativeKeyEvent.VC_SPACE:
                keyCode = KeyEvent.VK_SPACE;
                break;
            // End Alphanumeric Zone


            case NativeKeyEvent.VC_PRINTSCREEN:
                keyCode = KeyEvent.VK_PRINTSCREEN;
                break;

            case NativeKeyEvent.VC_SCROLL_LOCK:
                keyCode = KeyEvent.VK_SCROLL_LOCK;
                break;

            case NativeKeyEvent.VC_PAUSE:
                keyCode = KeyEvent.VK_PAUSE;
                break;


            // Begin Edit Key Zone
            case NativeKeyEvent.VC_INSERT:
                keyCode = KeyEvent.VK_INSERT;
                break;

            case NativeKeyEvent.VC_DELETE:
                keyCode = KeyEvent.VK_DELETE;
                break;

            case NativeKeyEvent.VC_HOME:
                keyCode = KeyEvent.VK_HOME;
                break;

            case NativeKeyEvent.VC_END:
                keyCode = KeyEvent.VK_END;
                break;

            case NativeKeyEvent.VC_PAGE_UP:
                keyCode = KeyEvent.VK_PAGE_UP;
                break;

            case NativeKeyEvent.VC_PAGE_DOWN:
                keyCode = KeyEvent.VK_PAGE_DOWN;
                break;
            // End Edit Key Zone


            // Begin Cursor Key Zone
            case NativeKeyEvent.VC_UP:
                keyCode = KeyEvent.VK_UP;
                break;
            case NativeKeyEvent.VC_LEFT:
                keyCode = KeyEvent.VK_LEFT;
                break;
            case NativeKeyEvent.VC_CLEAR:
                keyCode = KeyEvent.VK_CLEAR;
                break;
            case NativeKeyEvent.VC_RIGHT:
                keyCode = KeyEvent.VK_RIGHT;
                break;
            case NativeKeyEvent.VC_DOWN:
                keyCode = KeyEvent.VK_DOWN;
                break;
            // End Cursor Key Zone


            // Begin Numeric Zone
            case NativeKeyEvent.VC_NUM_LOCK:
                keyCode = KeyEvent.VK_NUM_LOCK;
                break;

            case NativeKeyEvent.VC_SEPARATOR:
                keyCode = KeyEvent.VK_SEPARATOR;
                break;
            // End Numeric Zone


            // Begin Modifier and Control Keys
            case NativeKeyEvent.VC_SHIFT:
                keyCode = KeyEvent.VK_SHIFT;
                break;

            case NativeKeyEvent.VC_CONTROL:
                keyCode = KeyEvent.VK_CONTROL;
                break;

            case NativeKeyEvent.VC_ALT:
                keyCode = KeyEvent.VK_ALT;
                break;

            case NativeKeyEvent.VC_META:
                keyCode = KeyEvent.VK_META;
                break;

            case NativeKeyEvent.VC_CONTEXT_MENU:
                keyCode = KeyEvent.VK_CONTEXT_MENU;
                break;
            // End Modifier and Control Keys


			/* Begin Media Control Keys
			case NativeKeyEvent.VC_POWER:
			case NativeKeyEvent.VC_SLEEP:
			case NativeKeyEvent.VC_WAKE:
			case NativeKeyEvent.VC_MEDIA_PLAY:
			case NativeKeyEvent.VC_MEDIA_STOP:
			case NativeKeyEvent.VC_MEDIA_PREVIOUS:
			case NativeKeyEvent.VC_MEDIA_NEXT:
			case NativeKeyEvent.VC_MEDIA_SELECT:
			case NativeKeyEvent.VC_MEDIA_EJECT:
			case NativeKeyEvent.VC_VOLUME_MUTE:
			case NativeKeyEvent.VC_VOLUME_UP:
			case NativeKeyEvent.VC_VOLUME_DOWN:
			case NativeKeyEvent.VC_APP_MAIL:
			case NativeKeyEvent.VC_APP_CALCULATOR:
			case NativeKeyEvent.VC_APP_MUSIC:
			case NativeKeyEvent.VC_APP_PICTURES:
			case NativeKeyEvent.VC_BROWSER_SEARCH:
			case NativeKeyEvent.VC_BROWSER_HOME:
			case NativeKeyEvent.VC_BROWSER_BACK:
			case NativeKeyEvent.VC_BROWSER_FORWARD:
			case NativeKeyEvent.VC_BROWSER_STOP:
			case NativeKeyEvent.VC_BROWSER_REFRESH:
			case NativeKeyEvent.VC_BROWSER_FAVORITES:
			// End Media Control Keys */


            // Begin Japanese Language Keys
            case NativeKeyEvent.VC_KATAKANA:
                keyCode = KeyEvent.VK_KATAKANA;
                break;

            case NativeKeyEvent.VC_UNDERSCORE:
                keyCode = KeyEvent.VK_UNDERSCORE;
                break;

            //case VC_FURIGANA:

            case NativeKeyEvent.VC_KANJI:
                keyCode = KeyEvent.VK_KANJI;
                break;

            case NativeKeyEvent.VC_HIRAGANA:
                keyCode = KeyEvent.VK_HIRAGANA;
                break;

            //case VC_YEN:
            // End Japanese Language Keys


            // Begin Sun keyboards
            case NativeKeyEvent.VC_SUN_HELP:
                keyCode = KeyEvent.VK_HELP;
                break;

            case NativeKeyEvent.VC_SUN_STOP:
                keyCode = KeyEvent.VK_STOP;
                break;

            //case VC_SUN_FRONT:

            //case VC_SUN_OPEN:

            case NativeKeyEvent.VC_SUN_PROPS:
                keyCode = KeyEvent.VK_PROPS;
                break;

            case NativeKeyEvent.VC_SUN_FIND:
                keyCode = KeyEvent.VK_FIND;
                break;

            case NativeKeyEvent.VC_SUN_AGAIN:
                keyCode = KeyEvent.VK_AGAIN;
                break;

            //case NativeKeyEvent.VC_SUN_INSERT:

            case NativeKeyEvent.VC_SUN_COPY:
                keyCode = KeyEvent.VK_COPY;
                break;

            case NativeKeyEvent.VC_SUN_CUT:
                keyCode = KeyEvent.VK_CUT;
                break;
        }
        return keyCode;
    }

    public static int translateModifier(int modifier) {
        int result = 0;
        if ((modifier & NativeKeyEvent.ALT_MASK) != 0) result |= 1 << 3;
        if ((modifier & NativeKeyEvent.META_MASK) != 0) result |= 1 << 2;
        if ((modifier & NativeKeyEvent.CTRL_MASK) != 0) result |= 1 << 1;
        if ((modifier & NativeKeyEvent.SHIFT_MASK) != 0) result |= 1;
        return result;
    }

    public static int translateButton(int oldCode) {
        int code = MouseEvent.BUTTON_NONE;
        switch (oldCode) {
            case NativeMouseEvent.BUTTON1:
                code = InputEvent.BUTTON1_MASK;
                break;
            case NativeMouseEvent.BUTTON2:
                code = InputEvent.BUTTON2_MASK;
                break;
            case NativeMouseEvent.BUTTON3:
                code = InputEvent.BUTTON3_MASK;
                break;
        }
        return code;
    }
}
