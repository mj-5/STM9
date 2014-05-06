package id.stsn.stm9.utility;

import java.util.Hashtable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QrCodeUtils {
    public final static QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

    /**
     * Generate Bitmap with QR Code based on input.
     * 
     * @param input
     * @param size
     * @return QR Code as Bitmap
     */
    public static Bitmap getQRCodeBitmap(final String input, final int size) {
        try {
            final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            final BitMatrix result = QR_CODE_WRITER.encode(input, BarcodeFormat.QR_CODE, size,
                    size, hints);

            final int width = result.getWidth();
            final int height = result.getHeight();
            final int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                final int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
//                    pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.TRANSPARENT;
                }
            }

            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
            
        } catch (final WriterException e) {
            Log.e("stm-9", "QrCodeUtils", e);
            return null;
        }
    }
    
    public static String getQrCodeText(final String path, final ImageView imgQr) {
    	Bitmap bMap = BitmapFactory.decodeFile(path);
    	imgQr.setImageBitmap(bMap);
    	
    	LuminanceSource source = new RGBLuminanceSources(bMap);
    	BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    	
    	Reader reader = new MultiFormatReader();
    	Result result = null;
    	
    	try {
			result = reader.decode(bitmap);
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ChecksumException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	String text = result.getText();
    	Log.i("decode result", text);
    	
    	return text;
    }

}
