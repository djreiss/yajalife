package djr.bugs;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.Properties;

/**
 * Class <code>YajaLifeControlPanel</code>
 *
 * @author <a href="mailto:reiss@uw.edu">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class YajaLifeControlPanel extends JFrame implements WindowListener, 
							    ActionListener {
   protected YajaLife world = null;
   protected JLabel label = null;
   
   YajaLifeControlPanel( YajaLife bw, String title ) {
      super( "Control Panel: " + title );
      this.world = bw;
      this.getContentPane().setLayout( new BorderLayout() );
      this.addWindowListener( this );
      JPanel jp = this.addInputComponents();
      this.getContentPane().add( "Center", jp );
      label = new JLabel( "Info:" );
      this.getContentPane().add( "South", label );
      this.pack();
   }

   protected JPanel addInputComponents() {
      JPanel jp = new JPanel();
      jp.setLayout( new GridLayout( 3, 6 ) );
      this.addInputComponent( new JButton( "Pause" ), jp );
      this.addInputComponent( new JButton( "Resources" ), jp );
      this.addInputComponent( new JButton( "Stats" ), jp );
      this.addInputComponent( new JButton( "Step" ), jp );
      this.addInputComponent( new JButton( "Save" ), jp );
      this.addInputComponent( new JButton( "Read" ), jp );
      this.addInputComponent( new JButton( "Screen Shot" ), jp );
      this.addInputComponent( new JButton( "Configure" ), jp );
      this.addInputComponent( new JButton( "Add" ), jp );
      this.addInputComponent( new JButton( "Clone" ), jp );
      this.addInputComponent( new JButton( "Close" ), jp );
      this.addInputComponent( new JButton( "Exit" ), jp );
      return jp;
   }

   protected void addInputComponent( JButton b, JPanel jp ) {
      jp.add( b );
      b.addActionListener( this );
   }

   public void actionPerformed( ActionEvent evt ) {
      world.doAction( evt, evt.getActionCommand() );
   }

   public void setText( String text ) {
      this.label.setText( text );
   }

   public void windowOpened( WindowEvent e ) { this.show(); }   
   public void windowClosing( WindowEvent e ) { this.hide(); }
   public void windowClosed( WindowEvent e ) { this.hide(); }
   public void windowIconified( WindowEvent e ) { }
   public void windowDeiconified( WindowEvent e ) { }
   public void windowActivated( WindowEvent e ) { }
   public void windowDeactivated( WindowEvent e ) { }
}
