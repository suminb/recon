package xyz.brogrammer.apps.recon.android

class Record(
        private val mac: String,
        private val ssid: String,
        private val signalStrengh: Int,
        private val frequency: Int,
        private val channelWidth: Int,
        private val timestamp: Long,
        private val capabilities: String) {

    override fun toString(): String {
        return "$mac ($ssid): $frequency:$signalStrengh:$channelWidth @$timestamp: $capabilities"
    }
}