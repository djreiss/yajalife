/* Decompiled by Mocha from DropArea.class */
/* Originally compiled from DropArea.java */

package djr.util.gui.awt;

import java.awt.*;

public class DropArea extends Panel
{
    static DropArea lastDropArea;
    boolean dragIsInside;

    public DropArea()
    {
        dragIsInside = false;
        setLayout(null);
    }

    public boolean mouseDrag(Event event, int i, int j)
    {
        if (event.target instanceof Draggable && lastDropArea != this)
        {
            lastDropArea = this;
            dragEnter(event);
            ((Draggable)event.target).dragEnter(this, event);
        }
        return false;
    }

    public boolean mouseUp(Event event, int i, int j)
    {
        if (event.target instanceof Draggable)
        {
            lastDropArea = null;
            dragCompleted(event);
            ((Draggable)event.target).dragCompleted(this, event, i, j);
        }
        return false;
    }

    public void dragEnter(Event event)
    {
        paint(getGraphics());
    }

    public void dragCompleted(Event event)
    {
        paint(getGraphics());
    }

    public void paint(Graphics g)
    {
        if (lastDropArea == this)
        {
            g.setColor(Color.cyan);
            g.fillRect(0, 0, size().width, size().height);
            return;
        }
        g.setColor(Color.black);
        g.drawRect(0, 0, size().width, size().height);
    }

    static 
    {
        lastDropArea = null;
    }
}
