package display;

import controllers.Recorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import static display.Editor.*;

public class KeyTimeline extends JComponent implements MouseWheelListener, MouseListener, MouseMotionListener {
    private final Editor editor;
    private final Recorder recorder;
    private final ArrayList<Integer> appearedKeyCodes;

    private int startDragPos;
    private long startDragTime;
    private int blockHeight;

    public KeyTimeline(Editor editor, Recorder recorder) {
        this.editor = editor;
        this.recorder = recorder;
        appearedKeyCodes = new ArrayList<>();
        setPreferredSize(new Dimension(640, 50));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        setMinimumSize(new Dimension(0, 50));
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    @Override
    public void paint(Graphics g) {
        Color old = g.getColor();
        int width = getWidth();
        int height = getHeight();
        g.setColor(old.darker());
        g.drawString(Strings.get().KEYS, 0, height - 4);
        g.setColor(old);
        g.drawLine(width / 2, 0, width / 2, height);
        g.drawLine(0, height - 1, width, height - 1);

        recorder.keyPresses.stream().filter(press -> press.time <= editor.currentTime + editor.timeRange && editor.currentTime - editor.timeRange <= press.time + press.duration).forEach(press -> {
            if (appearedKeyCodes.size() == 0) return;
            boolean selected = editor.selected.contains(press);
            if (selected) g.setColor(SELECTED_COLOR);
            Rectangle rectangle = getRectangle(press, blockHeight, appearedKeyCodes.indexOf(press.code), width, height, editor);
            g.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            if (selected) g.setColor(old);
        });
        g.drawString(String.valueOf(editor.timeRange / 1000d), 0, 10);

        g.setColor(SELECTED_COLOR);
        int leftLimit = mapToPos(0L, editor.currentTime, editor.timeRange, width);
        g.drawLine(leftLimit, 0, leftLimit, height);
        int rightLimit = mapToPos(editor.totalLength, editor.currentTime, editor.timeRange, width);
        g.drawLine(rightLimit, 0, rightLimit, height);
        g.setColor(old);
    }

    public void update() {
        appearedKeyCodes.clear();
        for (Recorder.Press press : recorder.keyPresses)
            if (!appearedKeyCodes.contains(press.code)) appearedKeyCodes.add(press.code);
        if (appearedKeyCodes.size() > 0) blockHeight = getHeight() / appearedKeyCodes.size();
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        editor.timeRange *= Math.pow(1.1, e.getPreciseWheelRotation());
        if (editor.timeRange < 128) editor.timeRange = 128;
        if (editor.timeRange > 32000) editor.timeRange = 32000;
        getParent().repaint();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        int width = getWidth();
        int height = getHeight();
        startDragPos = e.getX();
        startDragTime = editor.currentTime;
        if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
            editor.selected.clear();
            editor.offsetMap.clear();
        }
        recorder.keyPresses.stream()
                .filter(press -> press.time <= editor.currentTime + editor.timeRange && editor.currentTime - editor.timeRange <= press.time + press.duration)
                .filter(press -> getRectangle(press, blockHeight, appearedKeyCodes.indexOf(press.code), width, height, editor).contains(e.getPoint()))
                .forEach(editor.selected::add);
        editor.selected.stream().filter(obj -> obj instanceof Recorder.Press).forEach(press -> editor.offsetMap.put((Recorder.Press) press, e.getX() - mapToPos(press.time, editor.currentTime, editor.timeRange, width)));
        editor.repaint();
    }

    public void mouseDragged(MouseEvent e) {
        int width = getWidth();
        if (editor.selected.size() > 0)
            editor.selected.stream().filter(obj -> obj instanceof Recorder.Press)
                    .forEach(obj -> obj.time = mapToTime(e.getX() - editor.offsetMap.get(obj), width, editor.currentTime, editor.timeRange));
        else editor.currentTime = (long) (startDragTime - map(e.getX() - startDragPos, width / 2d, editor.timeRange));
        editor.repaint();
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }
}