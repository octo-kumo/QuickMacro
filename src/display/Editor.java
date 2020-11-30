package display;

import controllers.Recorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Editor extends JPanel implements MouseWheelListener {
    public static final Color SELECTED_COLOR = new Color(0xf05454);

    public final JMenuBar menuBar;
    public long currentTime = 0;
    public int timeRange = 1000; // 1 second
    public long totalLength;
    private final Recorder recorder;
    private final KeyTimeline keyTimeline;

    public final Map<Recorder.Press, Integer> offsetMap;
    public final ArrayList<Recorder.TimedObject> selected;
    private JMenuItem timeDisplay;

    public Editor() throws AWTException {
        recorder = new Recorder(this);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        selected = new ArrayList<>();
        offsetMap = new HashMap<>();
        keyTimeline = new KeyTimeline(this, recorder);
        MouseTimeline mouseTimeline = new MouseTimeline(this, recorder);
        MouseDisplay mouseDisplay = new MouseDisplay(this, recorder);
        menuBar = new JMenuBar() {{
            add(new JMenu(Strings.get().FILE) {{
                add(new JMenuItem(Strings.get().SAVE) {{
                    addActionListener(e -> recorder.saveToFile());
                }});
                add(new JMenuItem(Strings.get().OPEN) {{
                    addActionListener(e -> {
                        recorder.openFromFile();
                        Editor.this.update();
                    });
                }});
            }});
            add(new JMenu(Strings.get().HOTKEY) {{
                add(new Settings.HotkeyView(Strings.get().RECORD, Settings.RECORD_KEY, e -> recorder.record()));
                add(new Settings.HotkeyView(Strings.get().STOP, Settings.STOP_KEY, e -> {
                    recorder.stop();
                    Editor.this.update();
                }));
                add(new Settings.HotkeyView(Strings.get().PLAY, Settings.PLAY_KEY, e -> recorder.playBack()));
                add(new JMenuItem(Strings.get().CHANGE_HOTKEY_HINT));
            }});
            add(new JMenu(Strings.get().LANG) {{
                add(new JMenuItem("繁體中文") {{
                    addActionListener(e -> Strings.changeTo("zh"));
                }});
                add(new JMenuItem("English") {{
                    addActionListener(e -> Strings.changeTo("en"));
                }});
            }});

            add(timeDisplay = new JMenuItem() {{
                addActionListener(e -> {
                    int millis = (int) (currentTime % 1000), seconds = (int) (currentTime / 1000) % 60, minutes = (int) (currentTime / 60000) % 60, hours = (int) (currentTime / 3600000) % 24;
                    String new_time = JOptionPane.showInputDialog(Strings.get().SET_TIME_TO, (hours > 0 ? hours + ':' : "") + String.format("%02d:%02d:%03d", minutes, seconds, millis));
                    if (new_time == null) return;
                    if (new_time.contains(":") || new_time.contains("：")) {
                        String[] split = new_time.split("[:：]");
                        long total = Long.parseLong(split[split.length - 1]);
                        for (int i = 1; i < split.length; i++)
                            total += 1000 * Math.pow(60, i - 1) * Long.parseLong(split[split.length - i - 1]);
                        currentTime = total;
                    } else currentTime = Long.parseLong(new_time);
                    Editor.this.repaint();
                });
            }});
        }};
        add(keyTimeline);
        add(mouseTimeline);
        add(mouseDisplay);
        addMouseWheelListener(this);
    }

    @Override
    public void paint(Graphics g) {
        int millis = (int) (currentTime % 1000), seconds = (int) (currentTime / 1000) % 60, minutes = (int) (currentTime / 60000) % 60, hours = (int) (currentTime / 3600000) % 24;
        timeDisplay.setText((hours > 0 ? hours + ':' : "") + String.format("%02d:%02d:%03d", minutes, seconds, millis));
        timeDisplay.repaint();
        super.paint(g);
    }

    public void update() { // recording changed
        totalLength = Math.max(Math.max(lastObjectTime(recorder.keyPresses), lastObjectTime(recorder.mousePositions)), lastObjectTime(recorder.mousePresses));
        keyTimeline.update();
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        currentTime += timeRange * e.getPreciseWheelRotation() / 100;
        if (currentTime < 0) currentTime = 0;
        if (currentTime > totalLength) currentTime = totalLength;
        repaint();
    }

    public static Rectangle getRectangle(Recorder.Press press, int blockHeight, int index, int width, int height, Editor editor) {
        return new Rectangle(
                mapToPos(press.time, editor.currentTime, editor.timeRange, width),
                height - blockHeight - blockHeight * index,
                (int) map(press.duration, editor.timeRange, width / 2d),
                blockHeight
        );
    }

    public static int mapToPos(long time, long currentTime, long timeRange, int width) {
        return (int) (map(time - currentTime, timeRange, width / 2d) + width / 2);
    }

    public static long mapToTime(int pos, int width, long currentTime, long timeRange) {
        return (int) (map(pos - width / 2d, width / 2d, timeRange) + currentTime);
    }

    public static double map(double old, double range, double newRange) {
        return old * newRange / range;
    }

    public static long lastObjectTime(ArrayList<? extends Recorder.TimedObject> objects) {
        if (objects.size() == 0) return 0;
        Recorder.TimedObject object = objects.get(objects.size() - 1);
        if (object instanceof Recorder.Press) return object.time + ((Recorder.Press) object).duration;
        else return object.time;
    }

    public void openFile(File file) {
        recorder.openFromFile(file);
    }

    public void exit() {
        recorder.exit();
    }

    public void playbackCallback() {
        currentTime = recorder.currentTime;
        repaint();
    }
}
