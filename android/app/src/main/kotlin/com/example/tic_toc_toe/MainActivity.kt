package com.example.tic_toc_toe

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import com.google.gson.JsonElement
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.*

class MainActivity: FlutterActivity() {

    private val METHOD_CHANNEL = "game/exchange"

    private var pubNub : PubNub? = null
    private var channel_pubnub: String? = null
    private var handler : Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = Handler(Looper.getMainLooper())

        val pnConfiguration = PNConfiguration("MyUniqueUUID")
        pnConfiguration.subscribeKey = ""
        pnConfiguration.publishKey = ""

        pubNub = PubNub(pnConfiguration)

        pubNub.let{
            it?.addListener(object: SubscribeCallback(){
                override fun status(pubnub: PubNub, pnStatus: PNStatus) {}
                override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {}
                override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}
                override fun uuid(pubnub: PubNub, pnuuid: PNUUIDMetadataResult) {}
                override fun channel(pubnub: PubNub, pnchannel: PNChannelMetadataResult) {}
                override fun membership(pubnub: PubNub, pnMembership: PNMembershipResult) {}
                override fun messageAction(pubnub: PubNub, pnAction: PNMessageActionResult) {}
                override fun file(pubnub: PubNub, pnFile: PNFileEventResult) {}
                override fun message(pubnub: PubNub, pnMessage: PNMessageResult) {
                    Log.d("Pubnub Listener", "Received message: ${pnMessage.message.toString()}")

                    var receivedObject: String? = null
                    var actionReceived = "sendAction"

                    if(pnMessage.message.asJsonObject["tap"] !== null){
                        receivedObject = pnMessage.message.asJsonObject["tap"].toString()
                    }

                    handler?.let{
                        it.post{
                            val methodChannel = MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, METHOD_CHANNEL)
                            methodChannel.invokeMethod(actionReceived, receivedObject)
                        }
                    }
                }
            })
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val methodChannel = MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, METHOD_CHANNEL)
        methodChannel.setMethodCallHandler { call, result ->
            if(call.method == "sendAction"){
                pubNub!!.publish()
                    .message(call.arguments)
                    .channel(channel_pubnub)
                    .async{
                        _, status -> Log.d("Pubnub", "Teve erro? ${status.isError}")
                    }
                    result.success(true)
            }else if(call.method == "subscribe"){
                Log.d("MethodChannel", "Received call: ${call.method}")
                subscribeChannel(call.argument<String>("channel"))
                result.success(true)
            }
        }
    }

    private fun subscribeChannel(channelName: String?){
        channel_pubnub = channelName
        channelName.let{
            pubNub?.subscribe()?.channels(Arrays.asList(channelName))?.execute()
        }
    }
}
