import android.media.ExifInterface
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.Q)
private fun getFileLocation(path: String): Pair<Double, Double>? {
    try {
        val file = File(path)
        if (file.exists()) {
            val exif = ExifInterface(file)

            // Retrieve latitude and longitude from EXIF metadata
            val lat = exif.getAttributeDouble(ExifInterface.TAG_GPS_LATITUDE, 0.0)
            val lon = exif.getAttributeDouble(ExifInterface.TAG_GPS_LONGITUDE, 0.0)

            // Return only if both latitude and longitude are available
            if (lat != 0.0 && lon != 0.0) {
                return Pair(lat, lon)
            }
        }
    } catch (e: IOException) {
        Log.e("MainActivity", "Error reading EXIF data: ${e.message}")
    }
    return null // Return null if no location data is found
}
