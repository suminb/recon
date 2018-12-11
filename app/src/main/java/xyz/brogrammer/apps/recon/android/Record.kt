package xyz.brogrammer.apps.recon.android

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Record(
        private val mac: String,
        private val ssid: String,
        private val signalStrengh: Int,
        private val frequency: Int,
        private val channelWidth: Int,
        private val timestamp: Long,
        private val capabilities: String): Parcelable {

    override fun toString(): String {
        return "$mac ($ssid): $frequency:$signalStrengh:$channelWidth @$timestamp: $capabilities"
    }

    fun asCSV(): String {
        return "$timestamp, $mac, \"$ssid\", $frequency, $signalStrengh, \"$capabilities\"\n"
    }
}