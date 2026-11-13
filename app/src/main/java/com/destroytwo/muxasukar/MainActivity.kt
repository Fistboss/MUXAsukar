package com.destroytwo.muxasukar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.atan2
import kotlin.math.sqrt

// ---- Модели ----
data class Sugar(val id: Long, val x: Float, val y: Float)

class FlyBrain {
    var hunger = 1f
        private set

    fun pickTarget(sugar: Sugar?, screenW: Float, screenH: Float): PointF {
        return if (hunger > 0.5f && sugar != null)
            PointF(sugar.x, sugar.y)
        else
            PointF(
                (-200..screenW.toInt() + 200).random().toFloat(),
                (-200..screenH.toInt() + 200).random().toFloat()
            )
    }

    fun onEat() { hunger = 0f }
}

// ---- Activity ----
class MainActivity : AppCompatActivity() {

    private lateinit var mainlayout: FrameLayout
    private lateinit var muxa: ImageView
    private lateinit var flyView: FlyView
    private val brain = FlyBrain()

    private var flyX = 500f
    private var flyY = 500f

    private var currentAnimator: ValueAnimator? = null

    private var downX = 0f
    private var downY = 0f
    private val tapSlop = 10f

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        mainlayout = findViewById(R.id.frr)
        muxa = findViewById(R.id.imageView)
        SoundManager.init(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        flyView = FlyView(this)
        mainlayout.addView(flyView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        muxa.bringToFront()

     //   mainlayout.setOnTouchListener { _, event ->
     //       when (event.action) {
      //          MotionEvent.ACTION_DOWN -> {
////          //          true
          //      }
          //      MotionEvent.ACTION_UP -> {
          //          val dx = event.x - downX
          //          val dy = event.y - downY
           //         if (sqrt(dx * dx + dy * dy) < tapSlop) {
             //           flyView.addSugar(event.x, event.y)
            //        }true }else -> true } }

        muxa.post { startIdle() }
    }

    private fun startIdle() {
        muxa.postDelayed({ startFlight() }, (2000..4000).random().toLong())
    }

    private fun startFlight() {
        //val snd = playSound()
        SoundManager.playSound(0)
        val sugar = flyView.nearestSugar(flyX, flyY)
        val target = brain.pickTarget(sugar, mainlayout.width.toFloat(), mainlayout.height.toFloat())

        val startX = flyX
        val startY = flyY
        val dx = target.x - startX
        val dy = target.y - startY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val averageSpeed = (100..300).random().toFloat()
        val duration = (dist / averageSpeed * 1000f).toLong().coerceAtLeast(300L)

        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f

        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                flyX = startX + dx * f
                flyY = startY + dy * f
                muxa.x = flyX - muxa.width / 2f
                muxa.y = flyY - muxa.height / 2f
                muxa.rotation = angle
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (sugar != null) {
                        val ddx = sugar.x - flyX
                        val ddy = sugar.y - flyY
                        if (sqrt(ddx * ddx + ddy * ddy) < 30f) {
                            flyView.removeSugar(sugar)
                            brain.onEat()
                        }
                    }
                    SoundManager.stopSound()
                    startIdle()
                }

                override fun onAnimationStart(animation: Animator) {

                    //super.onAnimationStart(animation)
                }
            })
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentAnimator?.cancel()
    }
}


















// ---- View для стола и сахара ----
class FlyView(context: Context) : View(context) {
    private val sugars = mutableListOf<Sugar>()
    private var nextId = 0L
    private val bgPaint = Paint().apply { color = Color.rgb(245, 230, 200) }
    private val sugarPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }

    fun addSugar(x: Float, y: Float) {
        sugars.add(Sugar(nextId++, x, y))
        invalidate()
    }

    fun removeSugar(s: Sugar) {
        sugars.remove(s)
        invalidate()
    }

    fun nearestSugar(fx: Float, fy: Float): Sugar? = sugars.minByOrNull {
        val dx = it.x - fx
        val dy = it.y - fy
        sqrt(dx * dx + dy * dy)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        sugars.forEach { canvas.drawCircle(it.x, it.y, 20f, sugarPaint) }
    }
}