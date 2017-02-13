package djr.nr;

/**
 * Interface <code>MutableFunction</code>
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public interface MutableFunction extends Function {
   /* Rearrange the old parameters into a new set to try. */
   public MutableFunction mutate( MutableFunction into );
}
