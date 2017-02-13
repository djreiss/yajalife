package djr.util.gui.imagefilters;

import java.awt.Color;
import java.awt.image.RGBImageFilter;
import java.awt.image.ImageProducer;

/**
 * The HueFilter class implements in ImageFilter which recolors
 * the pixels in an image to have a new primary hue.
 */
public class HueFilter extends RGBImageFilter {
    /*
     * A private variable used to hold hue/saturation/brightness
     * values returned from the static conversion methods in Color.
     */
    private float hsbvals[] = new float[3];

    /**
     * the Hue of the indicated new foreground color.
     */
    float fgHue;

    /**
     * the Saturation of the indicated new foreground color.
     */
    float fgSaturation;

    /**
     * the Brightness of the indicated new foreground color.
     */
    float fgBrightness;

    /**
     * Construct a HueFilter object which performs color modifications
     * to warp existing image colors to have a new primary hue.
     */
    public HueFilter(Color fg) {
	Color.RGBtoHSB(fg.getRed(), fg.getGreen(), fg.getBlue(), hsbvals);
	fgHue        = hsbvals[0];
	fgSaturation = hsbvals[1];
	fgBrightness = hsbvals[2];
	canFilterIndexColorModel = true;
    }

    /**
     * Filter an individual pixel in the image by modifying its
     * hue, saturation, and brightness components to be similar
     * to the indicated new foreground color.
     */
    public int filterRGB(int x, int y, int rgb) {
	int alpha = (rgb >> 24) & 0xff;
	int red   = (rgb >> 16) & 0xff;
	int green = (rgb >>  8) & 0xff;
	int blue  = (rgb      ) & 0xff;
	Color.RGBtoHSB(red, green, blue, hsbvals);
	float newHue = fgHue;
	float newSaturation = hsbvals[1] * fgSaturation;
	float newBrightness = hsbvals[2] *
	    (hsbvals[1] * fgBrightness + (1 - hsbvals[1]));
	rgb = Color.HSBtoRGB(newHue, newSaturation, newBrightness);
	return (rgb & 0x00ffffff) | (alpha << 24);
    }
}
