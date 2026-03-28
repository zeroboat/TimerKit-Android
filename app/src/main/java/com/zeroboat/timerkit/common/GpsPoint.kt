package com.zeroboat.timerkit.common

/** UI 레이어와 독립적으로 GPS 좌표를 저장. 지도 표시 시 LatLng으로 변환 */
data class GpsPoint(val lat: Double, val lon: Double)
