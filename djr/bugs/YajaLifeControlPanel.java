package djr.bugs;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Properties;

/**
 * Class <code>YajaLifeControlPanel</code>
 *
 * @author <a href="mailto:astrodud@">astrodud</a>
 * @version 1.0
 */
public class YajaLifeControlPanel extends Frame implements WindowListener {
   protected YajaLife world = null;
   
   YajaLifeControlPanel( YajaLife bw ) {
      super( "Control Panel" );
      this.world = bw;
      this.setLayout( new FlowLayout() );
      this.setBounds( 501, 0, 300, 500 );
      this.addWindowListener( this );
      this.addInputComponents();
      this.pack();
   }

   protected void addInputComponents() {
      this.add( new Button( "Pause" ) );
      this.add( new Button( "Resources" ) );
      this.add( new Button( "Stats" ) );
      this.add( new Button( "Step" ) );
      this.add( new Button( "Save" ) );
      this.add( new Button( "Read" ) );
      this.add( new Button( "Screen Shot" ) );
      this.add( new Button( "Close" ) );
      this.add( new Button( "Exit" ) );
   }

   public boolean action( Event evt, Object what ) {
      return world.action( evt, what );
   }

   public void windowOpened( WindowEvent e ) { this.show(); }   
   public void windowClosing( WindowEvent e ) { this.hide(); }
   public void windowClosed( WindowEvent e ) { this.hide(); }
   public void windowIconified( WindowEvent e ) { }
   public void windowDeiconified( WindowEvent e ) { }
   public void windowActivated( WindowEvent e ) { }
   public void windowDeactivated( WindowEvent e ) { }
}
