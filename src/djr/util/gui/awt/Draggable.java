package djr.util.gui.awt;

import java.awt.Event;

public interface Draggable
{
    public abstract void dragEnter(DropArea dropArea, Event event);
    public abstract void dragCompleted(DropArea dropArea, Event event, int i, int j);
}
