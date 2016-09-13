package org.lateralgm.file.iconio;

import org.lateralgm.file.StreamDecoder;
import org.lateralgm.file.StreamEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class BitmapPNG extends AbstractBitmap {
	private BufferedImage image;

	public BitmapPNG(BitmapDescriptor descriptor) {
		super(descriptor);
	}

	public BufferedImage createImageRGB() {
		return image;
	}

	void read(StreamDecoder dec) throws IOException {
		image = ImageIO.read(dec);
	}

	@Override
	void write(StreamEncoder out) throws IOException {
		ImageIO.write(image, "png", out);
	}
}
