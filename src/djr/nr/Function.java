package djr.nr;

/**
 * Interface <code>Function</code>
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public interface Function extends java.io.Serializable {
   public double evaluate();
   public Function duplicate();
}
