package dev.brahmkshatriya.echo.utils

//
//abstract class GesturesListener : {
//    private var timer: Timer? = null
//    private val delay: Long = 200
//
//    override fun onSingleTapUp(e: MotionEvent): Boolean {
//        return  onSingleClick(e)
//    }
//
//    override fun onDoubleTap(e: MotionEvent): Boolean {
//        return processDoubleClickEvent(e)
//    }
//
//    private val handler = Handler(Looper.getMainLooper())
//    private fun processSingleClickEvent(e: MotionEvent) {
//        timer = Timer().apply {
//            schedule(delay) {
//                handler.post { }
//            }
//        }
//    }
//
//    private fun processDoubleClickEvent(e: MotionEvent): Boolean {
//        timer?.apply {
//            cancel()
//            purge()
//        }
//        return onDoubleClick(e)
//    }
//
//    open fun onSingleClick(event: MotionEvent): Boolean {
//        return false
//    }
//
//    open fun onDoubleClick(event: MotionEvent): Boolean {
//        return false
//    }
//}