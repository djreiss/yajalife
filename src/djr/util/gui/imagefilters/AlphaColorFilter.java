package djr.util.gui.imagefilters;

import java.awt.Color;
import java.awt.image.RGBImageFilter;

/**
 * The AlphaColorFilter class implements in ImageFilter which
 * interpolates the colors in an image between a background
 * color and a foreground color based on the intensity of the
 * color components in the source image at that location.
 */
public class AlphaColorFilter extends RGBImageFilter {
    /**
     * The foreground color used to replace the colors in the
     * image wherever the color intensities are brightest.
     */
    Color fgColor;

    /**
     * The background color used to replace the colors in the
     * image wherever the color intensities are darkest.
     */
    Color bgColor;

    /**
     * Construct an AlphaColorFilter object which performs
     * interpolation filtering between the specified foreground
     * and background colors.
     */
    public AlphaColorFilter(Color fg, Color bg) {
		fgColor = fg;
		bgColor = bg;
		canFilterIndexColorModel = true;
    }

    /**
     * Interpolate a color component, given a foreground, background
     * and alpha value components.
     */
    private int interpolate(int fg, int bg, int alpha) {
		int newval = (((fg - bg) * alpha) / 255) + bg;
		return newval;
    }

    /**
     * Filter an individual pixel in the image by interpolating
     * between the foreground and background colors depending on
     * the brightness of the pixel in the source image.
     */
    public int filterRGB(int x, int y, int rgb) {
		int alpha = (rgb >> 24) & 0xff;
		int red   = (rgb >> 16) & 0xff;
		int green = (rgb >>  8) & 0xff;
		int blue  = (rgb      ) & 0xff;
		red   = interpolate(fgColor.getRed(),   bgColor.getRed(),   red);
		green = interpolate(fgColor.getGreen(), bgColor.getGreen(), green);
		blue  = interpolate(fgColor.getBlue(),  bgColor.getBlue(),  blue);
		return (alpha << 24) | (red << 16) | (green << 8) | (blue);
    }
}
