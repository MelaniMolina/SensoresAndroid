package com.projects.enzoftware.metalball

import android.app.Service
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.google.firebase.firestore.CollectionReference


class MetalBall : AppCompatActivity() , SensorEventListener {

    private var mSensorManager : SensorManager ?= null
    private var mAccelerometer : Sensor ?= null
    var ground : GroundView ?= null
    private var firestore: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        // get reference of the service
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // focus in accelerometer
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // setup the window
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Inicializar Firestore
        firestore = FirebaseFirestore.getInstance()

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            window.decorView.systemUiVisibility =   View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            View.SYSTEM_UI_FLAG_FULLSCREEN
            View.SYSTEM_UI_FLAG_IMMERSIVE
        }

        // set the view
        ground = GroundView(this)
        setContentView(ground)
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            ground!!.updateMe(event.values[1] , event.values[0])
        }
    }

     override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this,mAccelerometer,
                                        SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager!!.unregisterListener(this)
    }

    class DrawThread (surfaceHolder: SurfaceHolder , panel : GroundView) : Thread() {
        private var surfaceHolder :SurfaceHolder ?= null
        private var panel : GroundView ?= null
        private var run = false

        init {
            this.surfaceHolder = surfaceHolder
            this.panel = panel
        }

        fun setRunning(run : Boolean){
            this.run = run
        }

        override fun run() {
            var c: Canvas ?= null
            while (run){
                c = null
                try {
                    c = surfaceHolder!!.lockCanvas(null)
                    synchronized(surfaceHolder!!){
                        panel!!.draw(c)
                    }
                }finally {
                    if (c!= null){
                        surfaceHolder!!.unlockCanvasAndPost(c)
                    }
                }
            }
        }

    }

}


class GroundView(context: Context?) : SurfaceView(context), SurfaceHolder.Callback {


    var firestore: FirebaseFirestore? = FirebaseFirestore.getInstance()
    // Después de obtener la referencia a la colección y al documento
    val docRef = firestore?.collection("ubicaciones")?.document("esfera")
    init {

        firestore = FirebaseFirestore.getInstance()

    }


    // ball coordinates
    var cx : Float = 10.toFloat()
    var cy : Float = 10.toFloat()

    // last position increment

    var lastGx : Float = 0.toFloat()
    var lastGy : Float = 0.toFloat()

    // graphic size of the ball

    var picHeight: Int = 0
    var picWidth : Int = 0

    var icon:Bitmap ?= null

    // window size

    var Windowwidth : Int = 0
    var Windowheight : Int = 0

    // is touching the edge ?

    var noBorderX = false
    var noBorderY = false

    var vibratorService : Vibrator ?= null
    var thread : MetalBall.DrawThread?= null



    init {
        holder.addCallback(this)
        //create a thread
        thread = MetalBall.DrawThread(holder, this)
        // get references and sizes of the objects
        val display: Display = (getContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size:Point = Point()
        display.getSize(size)
        Windowwidth = size.x
        Windowheight = size.y
        icon = BitmapFactory.decodeResource(resources,R.drawable.ball)
        picHeight = icon!!.height
        picWidth = icon!!.width
        vibratorService = (getContext().getSystemService(Service.VIBRATOR_SERVICE)) as Vibrator
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Implementación de surfaceChanged
    }


    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Implementación de surfaceDestroyed
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread!!.setRunning(true)
        thread!!.start()
    }
    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        if (canvas != null){
            canvas.drawColor(0xFFAAAAA)
            icon?.let { canvas.drawBitmap(it,cx,cy,null) }
        }
    }

    override public fun onDraw(canvas: Canvas?) {

        if (canvas != null){
            canvas.drawColor(0xFFAAAAA)
            icon?.let { canvas.drawBitmap(it,cx,cy,null) }
        }
    }

    fun updateMe(inx: Float, iny: Float) {
        lastGx += inx
        lastGy += iny

        val newCx = cx + lastGx
        val newCy = cy + lastGy

        if (newCx > (Windowwidth - picWidth)) {
            cx = (Windowwidth - picWidth).toFloat()
            lastGx = 0F
            if (noBorderX) {
                vibratorService!!.vibrate(100)
                noBorderX = false
            }
        } else if (newCx < 0) {
            cx = 0F
            lastGx = 0F
            if (noBorderX) {
                vibratorService!!.vibrate(100)
                noBorderX = false
            }
        } else {
            noBorderX = true
            cx = newCx
        }

        if (newCy > (Windowheight - picHeight)) {
            cy = (Windowheight - picHeight).toFloat()
            lastGy = 0F
            if (noBorderY) {
                vibratorService!!.vibrate(100)
                noBorderY = false
            }
        } else if (newCy < 0) {
            cy = 0F
            lastGy = 0F
            if (noBorderY) {
                vibratorService!!.vibrate(100)
                noBorderY = false
            }
        } else {
            noBorderY = true
            cy = newCy
        }

        invalidate()

        // Agrega un listener para escuchar cambios en el documento
        docRef?.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("TAG", "Error al escuchar cambios en Firestore", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // Documento existe, actualiza la posición local según los datos de Firestore
                val newCx = snapshot.getDouble("cx")
                val newCy = snapshot.getDouble("cy")

                // Actualiza la posición local
                if (newCx != null && newCy != null) {
                    cx = newCx.toFloat()
                    cy = newCy.toFloat()
                    invalidate()
                }
            } else {
                Log.d("TAG", "Documento 'esfera' no existe")
            }
        }
    }



}
