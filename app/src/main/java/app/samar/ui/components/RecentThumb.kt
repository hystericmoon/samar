package app.samar.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import app.samar.ui.theme.PhotoPlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A Recent thumbnail, square-cropped. Shows the placeholder colour while it loads. */
@Composable
fun RecentThumb(file: File?, modifier: Modifier = Modifier) {
    val image by produceState<ImageBitmap?>(null, file) {
        value = file?.let { f ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() }
        }
    }
    Box(modifier.fillMaxSize().background(PhotoPlaceholder.A)) {
        image?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}
