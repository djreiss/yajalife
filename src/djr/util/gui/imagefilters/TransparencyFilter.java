package djr.util.gui.imagefilters;

import java.awt.image.RGBImageFilter;

/**
 * The TransparencyFilter class implements in ImageFilter which
 * sets the alpha to a value between 0 and 255 for transparency.
 */
public class TransparencyFilter extends RGBImageFilter {
    /**
     * Store the desired amount of transparency
     */
	int transparency;

    /**
     * Construct an TransparencyFilter object with set amount of transparency
     */
    public TransparencyFilter( int transparency ) {
		this.transparency = transparency;
		if ( this.transparency < 0 ) this.transparency = 0;
		if ( this.transparency > 255 ) this.transparency = 255;
		//canFilterIndexColorModel = true;
    }

    /**
     * Set the alpha to the desired transparency value (unless it's already
     * 100% transparent as in a transparent GIF)
     */
    public int filterRGB(int x, int y, int rgb) {
		int alpha = (rgb >> 24) & 0xff;
		if ( alpha != 0 ) alpha = transparency & 0xff;
		return ( rgb & 0x00ffffff ) | ( alpha << 24 );
    }
}
