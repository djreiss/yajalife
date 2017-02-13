package djr.bugs;
import java.applet.*;
import java.awt.*;

/**
 * Class <code>YajaLifeApplet</code>
 *
 * @author <a href="mailto:djreiss@">djreiss</a>
 * @version 1.0
 */
public class YajaLifeApplet extends Applet {
   public static YajaLifeApplet applet = null;
   public YajaLife world = null;

   public void init() {
      if ( applet == null ) applet = this;
      world = new YajaLife( getCodeBase().toString() );
      setLayout( new BorderLayout() );
      this.add( "Center", world );
   }

   public void start() {
      world.start();
   }

   public void stop() {
      world.stop();
   }

   public void destroy() {
      this.remove( world );
      world.destroy();
      world = null;
   }
}
