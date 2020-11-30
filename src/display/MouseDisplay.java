package display;

import controllers.Recorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import static display.Editor.SELECTED_COLOR;
import static display.Editor.map;

public class MouseDisplay extends JComponent implements MouseListener, MouseMotionListener, ComponentListener {
    public static int circleRadius = 2;

    private final Editor editor;
    private final Recorder recorder;

    private final Map<Recorder.MousePos, Point> offsetMap;
    private final Point startDragPos;
    private int width;
    private int height;
    private double sWidth;
    private double sHeight;

    public MouseDisplay(Editor editor, Recorder recorder) {
        this.editor = editor;
        this.recorder = recorder;
        setPreferredSize(new Dimension(640, 480));
        addMouseListener(this);
        addMouseMotionListener(this);
        addComponentListener(this);

        offsetMap = new HashMap<>();
        startDragPos = new Point(0, 0);

        width = getWidth();
        height = getHeight();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        sWidth = screenSize.getWidth();
        sHeight = screenSize.getHeight();
    }

    @Override
    public void paint(Graphics g) {
        ArrayList<Recorder.ScreenState> screenStates = recorder.screenStates;
        if (screenStates.size() > 1) {
            OptionalInt any = IntStream.range(0, screenStates.size() - 1).filter(i -> screenStates.get(i).time <= editor.currentTime && screenStates.get(i + 1).time > editor.currentTime).findAny();
            if (any.isPresent()) g.drawImage(screenStates.get(any.getAsInt()).getImage(), 0, 0, width, height, null);
            else if (Math.abs(editor.currentTime - screenStates.get(0).time) < Math.abs(editor.currentTime - screenStates.get(screenStates.size() - 1).time))
                g.drawImage(screenStates.get(0).getImage(), 0, 0, width, height, null);
            else g.drawImage(screenStates.get(screenStates.size() - 1).getImage(), 0, 0, width, height, null);
        }
        Color color = g.getColor();
        ArrayList<Recorder.MousePos> mousePositions = recorder.mousePositions;
        IntStream.range(0, mousePositions.size()).filter(i -> Math.abs(mousePositions.get(i).time - editor.currentTime) < editor.timeRange).forEach(i -> {
            Recorder.MousePos pos = mousePositions.get(i);
            if (i != 0) {
                Recorder.MousePos prev = mousePositions.get(i - 1);
                if (editor.currentTime >= prev.time && editor.currentTime < pos.time) drawCursor(g, prev, pos, color);
                g.drawLine((int) map(pos.x, sWidth, width), (int) map(pos.y, sHeight, height), (int) map(prev.x, sWidth, width), (int) map(prev.y, sHeight, height));
            }
            g.drawOval((int) map(pos.x, sWidth, width) - circleRadius, (int) map(pos.y, sHeight, height) - circleRadius, circleRadius * 2, circleRadius * 2);
        });
        g.setColor(SELECTED_COLOR);
        editor.selected.stream().filter(object -> object instanceof Recorder.MousePos).forEach(object -> g.drawOval((int) map(((Recorder.MousePos) object).x, sWidth, width) - circleRadius, (int) map(((Recorder.MousePos) object).y, sHeight, height) - circleRadius, circleRadius * 2, circleRadius * 2));
        g.setColor(color);
    }

    private void drawCursor(Graphics g, Recorder.MousePos prev, Recorder.MousePos pos, Color defaultColor) {
        double dt = (editor.currentTime - prev.time) * 1.0 / (pos.time - prev.time);
        double dx = pos.x - prev.x;
        double dy = pos.y - prev.y;
        Point currentPoint = new Point((int) map(prev.x + dt * dx, sWidth, width), (int) map(prev.y + dt * dy, sHeight, height));
        g.setColor(SELECTED_COLOR);
        g.fillOval(currentPoint.x - circleRadius - 1, currentPoint.y - circleRadius - 1, circleRadius * 2 + 2, circleRadius * 2 + 2);
        g.setColor(defaultColor);
    }

    public void mousePressed(MouseEvent e) {
        if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
            editor.selected.clear();
            offsetMap.clear();
        }
        ArrayList<Recorder.MousePos> mousePositions = recorder.mousePositions;
        mousePositions.stream()
                .filter(pos -> Math.abs(pos.time - editor.currentTime) < editor.timeRange && Math.hypot(map(pos.x, sWidth, width) - e.getX(), map(pos.y, sHeight, height) - e.getY()) < circleRadius + 0.5)
                .forEach(editor.selected::add);
        editor.selected.stream().filter(obj -> obj instanceof Recorder.MousePos).forEach(pos -> offsetMap.put((Recorder.MousePos) pos, new Point(((Recorder.MousePos) pos).x - (int) map(e.getX(), width, sWidth), ((Recorder.MousePos) pos).y - (int) map(e.getY(), height, sHeight))));
        startDragPos.setLocation(map(e.getX(), width, sWidth), map(e.getY(), height, sHeight));
        editor.repaint();
    }


    public void mouseDragged(MouseEvent e) {
        Point newPos = new Point((int) map(e.getX(), width, sWidth), (int) map(e.getY(), height, sHeight));
        editor.selected.stream().filter(object -> object instanceof Recorder.MousePos).forEach(obj -> {
            Point offset = offsetMap.get(obj);
            ((Recorder.MousePos) obj).x = newPos.x + offset.x;
            ((Recorder.MousePos) obj).y = newPos.y + offset.y;
        });
        repaint();
    }


    public void componentResized(ComponentEvent e) {
        width = getWidth();
        height = getHeight();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        sWidth = screenSize.getWidth();
        sHeight = screenSize.getHeight();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }
}