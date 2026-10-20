package app.samar.effects

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import androidx.annotation.RequiresApi

/**
 * Smear on Android 13+: the [HeadSmear] recipe as an AGSL shader. AGSL only runs on the GPU,
 * so the patch is drawn off-screen with a HardwareRenderer and read back into a normal Bitmap
 * (with alpha: the smear's colour, and how much of it to show).
 */
@RequiresApi(33)
object SmearShader {

    // Same steps as HeadSmear's CPU version; streak windows are sampled with taps instead of
    // running sums. Keep the numbers in step with HeadSmear.
    private const val AGSL = """
        uniform shader image;     // the photo, in photo pixels
        uniform float2 origin;    // patch top-left, photo pixels
        uniform float2 centre;    // face centre
        uniform float2 radii;     // face half-width, half-height
        uniform float up;         // how far the head reaches above the face box
        uniform float strength;   // streak length
        uniform float spill;      // how far tails run past the head
        uniform float fine;
        uniform float coarse;
        uniform float seed;
        uniform float isRect;

        float hash(float n, float ch) { return fract(sin(n * 12.9898 + ch * 78.233 + seed * 37.719) * 43758.5453); }

        float noise(float t, float ch) {
            float i = floor(t);
            float f = t - i;
            float u = f * f * (3.0 - 2.0 * f);
            return mix(hash(i, ch), hash(i + 1.0, ch), u);
        }

        float shapeDist(float dx, float dy) {
            return isRect > 0.5 ? max(abs(dx), abs(dy)) : sqrt(dx * dx + dy * dy);
        }

        float headMask(float2 q) {
            float rbm = q.y < centre.y ? radii.y + up : radii.y;
            return 1.0 - smoothstep(0.85, 1.05, shapeDist((q.x - centre.x) / radii.x, (q.y - centre.y) / rbm));
        }

        // Average head colour (rgb) and head coverage (a) along a sideways window.
        float4 finish(float3 c, float m, float taps) {
            return m > 0.0001 ? float4(c / m, m / taps) : float4(0.0);
        }

        float4 streak32(float y, float centreX, float len) {
            float3 c = float3(0.0);
            float m = 0.0;
            for (int i = 0; i < 32; i++) {
                float2 q = float2(centreX + ((float(i) + 0.5) / 32.0 - 0.5) * len, y);
                float w = headMask(q);
                c += w * image.eval(q).rgb;
                m += w;
            }
            return finish(c, m, 32.0);
        }

        float4 streak8(float y, float centreX, float len) {
            float3 c = float3(0.0);
            float m = 0.0;
            for (int i = 0; i < 8; i++) {
                float2 q = float2(centreX + ((float(i) + 0.5) / 8.0 - 0.5) * len, y);
                float w = headMask(q);
                c += w * image.eval(q).rgb;
                m += w;
            }
            return finish(c, m, 8.0);
        }

        half4 main(float2 p) {
            float2 a = p + origin;
            float v = a.y - centre.y;
            float len = strength * (0.5 + 0.7 * noise(v / fine, 1.0) + 0.3 * noise(v / coarse, 2.0));
            float drift = strength * 0.18 * (noise(v / coarse, 3.0) - 0.5) * 2.0;
            float reach = spill * (0.7 + 0.45 * noise(v / coarse, 4.0) + 0.15 * noise(v / fine, 5.0));

            // Main streak, then the head caught at two other positions (ghosts).
            float4 s0 = streak32(a.y, a.x + drift, max(1.0, len));
            float4 s1 = streak8(a.y, a.x + drift + strength * 0.3, max(1.0, len * 0.2));
            float4 s2 = streak8(a.y, a.x + drift - strength * 0.26, max(1.0, len * 0.2));
            float w0 = 0.6 * s0.a, w1 = 0.22 * s1.a, w2 = 0.18 * s2.a;
            float cov = w0 + w1 + w2;
            if (cov <= 0.0001) return half4(0.0);
            float3 col = (w0 * s0.rgb + w1 * s1.rgb + w2 * s2.rgb) / cov * 0.93;

            float dx = (a.x - centre.x) / radii.x;
            float dy = (a.y - centre.y) / radii.y;
            float d = shapeDist(dx, dy);
            float shown = min(1.0, cov * 1.15);
            float alpha;
            if (d <= 1.0) {
                alpha = shown + (1.0 - shown) * smoothstep(0.0, 0.25, 1.0 - d);
            } else {
                float hw = abs(dy) < 1.0 ? (isRect > 0.5 ? radii.x : radii.x * sqrt(1.0 - dy * dy)) : 0.0;
                alpha = shown * (1.0 - smoothstep(0.0, reach * 1.3, abs(a.x - centre.x) - hw));
            }
            return half4(half3(col * alpha), half(alpha));
        }
    """

    fun render(base: Bitmap, p: HeadSmear.Params): Bitmap {
        val w = p.patch.width()
        val h = p.patch.height()
        val src = p.source
        val piece = Bitmap.createBitmap(base, src.left, src.top, src.width(), src.height())
        val image = BitmapShader(piece, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply { setTranslate(src.left.toFloat(), src.top.toFloat()) })
        }
        val shader = RuntimeShader(AGSL).apply {
            setInputShader("image", image)
            setFloatUniform("origin", p.patch.left.toFloat(), p.patch.top.toFloat())
            setFloatUniform("centre", p.cx, p.cy)
            setFloatUniform("radii", p.ra, p.rb)
            setFloatUniform("up", p.up)
            setFloatUniform("strength", p.strength)
            setFloatUniform("spill", p.spill)
            setFloatUniform("fine", p.fine)
            setFloatUniform("coarse", p.coarse)
            setFloatUniform("seed", p.seed)
            setFloatUniform("isRect", if (p.rect) 1f else 0f)
        }

        val reader = ImageReader.newInstance(
            w, h, PixelFormat.RGBA_8888, 1,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
        )
        val node = RenderNode("smear").apply { setPosition(0, 0, w, h) }
        val renderer = HardwareRenderer()
        try {
            val canvas = node.beginRecording()
            canvas.drawColor(Color.TRANSPARENT, BlendMode.CLEAR)
            canvas.drawPaint(Paint().apply { this.shader = shader })
            node.endRecording()

            renderer.isOpaque = false // keep the alpha: it says how much smear to show
            renderer.setSurface(reader.surface)
            renderer.setContentRoot(node)
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()

            reader.acquireNextImage().use { img ->
                val buffer = img.hardwareBuffer ?: error("No buffer")
                buffer.use {
                    val hw = Bitmap.wrapHardwareBuffer(it, ColorSpace.get(ColorSpace.Named.SRGB))
                        ?: error("Could not wrap buffer")
                    return hw.copy(Bitmap.Config.ARGB_8888, false)
                }
            }
        } finally {
            renderer.destroy()
            node.discardDisplayList()
            reader.close()
            piece.recycle()
        }
    }
}
