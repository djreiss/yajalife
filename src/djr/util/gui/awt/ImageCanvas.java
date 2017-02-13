package djr.util.gui.awt;

import java.awt.*;
import java.awt.image.*;
import java.net.URL;

public class ImageCanvas extends Canvas
{
    Image mOriginalImage;
    Image mFilteredImage;
    ImageFilter mFilter;

    public ImageCanvas(Image image)
    {
        mOriginalImage = image;
        useFilter(null, false);
    }

    public ImageCanvas(Image image, ImageFilter imageFilter)
    {
        mOriginalImage = image;
        useFilter(imageFilter, false);
    }

    public ImageCanvas(URL uRL)
    {
        mOriginalImage = getToolkit().getImage(uRL);
        djr.util.gui.AWTUtils.waitForImage(mOriginalImage, this);
        useFilter(null, false);
    }

    public ImageCanvas(URL uRL, ImageFilter imageFilter)
    {
        mOriginalImage = getToolkit().getImage(uRL);
        djr.util.gui.AWTUtils.waitForImage(mOriginalImage, this);
        useFilter(imageFilter, false);
    }

    public void useFilter(ImageFilter imageFilter, boolean flag)
    {
        mFilter = imageFilter;
        mFilteredImage = null;
        if (imageFilter == null)
            mFilteredImage = mOriginalImage;
        else
        {
            FilteredImageSource filteredImageSource = new FilteredImageSource(mOriginalImage.getSource(), imageFilter);
            mFilteredImage = createImage(filteredImageSource);
        }
        if (size().width == 0 || size().height == 0)
        {
            magnify(1.0, 1.0);
            return;
        }
        if (getParent() != null)
            repaint();
    }

    public void paint(Graphics g)
    {
        update(g);
    }

    public void update(Graphics g)
    {
        Image image = (mFilteredImage != null) ? mFilteredImage : mOriginalImage;
        if (image == null || (checkImage(image, this) & 64) != 0)
        {
            g.setColor(Color.red);
            g.fillRect(0, 0, size().width, size().height);
            return;
        }
        g.translate(-location().x, -location().y);
        getParent().paint(g);
        g.translate(location().x, location().y);
        g.drawImage(image, 0, 0, size().width, size().height, this);
    }

    public void magnify(double d1, double d2)
    {
        Image image = (mFilteredImage != null) ? mFilteredImage : mOriginalImage;
        if (mFilteredImage != null && (image.getWidth(this) == 0 || image.getHeight(this) == 0))
            image = mOriginalImage;
        if (image != null && d1 != 0.0 && d2 != 0.0 && image.getWidth(this) != 0 && image.getHeight(this) != 0)
        {
            int i = (int)(d1 * image.getWidth(this));
            int j = (int)(d2 * image.getHeight(this));
            resize(i, j);
        }
        if (getParent() != null)
            repaint();
    }

    public Image getImage()
    {
        return mFilteredImage;
    }

    public void setImage(Image image)
    {
        mOriginalImage = image;
        useFilter(mFilter, true);
    }
}
