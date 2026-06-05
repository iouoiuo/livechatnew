package tech.mastersam.livechat;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;

import com.livechatinc.chatsdk.LiveChat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import java.util.HashMap;

public class LivechatPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    private MethodChannel methodChannel;
    private EventChannel eventChannel;
    private Context applicationContext;
    private Activity activity;
    private EventChannel.EventSink events;
    private String initializedLicense;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        this.applicationContext = flutterPluginBinding.getApplicationContext();
        setupChannel(flutterPluginBinding.getBinaryMessenger());

        flutterPluginBinding.getPlatformViewRegistry().registerViewFactory(
                "embedded_chat_view",
                new EmbeddedChatViewFactory()
        );
    }

    private void setupChannel(BinaryMessenger messenger) {
        methodChannel = new MethodChannel(messenger, "livechatt");
        methodChannel.setMethodCallHandler(this);

        eventChannel = new EventChannel(messenger, "livechatt/events");
        eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink eventSink) {
                events = eventSink;
            }

            @Override
            public void onCancel(Object arguments) {
                events = null;
            }
        });
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;
            case "beginChat":
                handleBeginChat(call, result);
                break;
            case "clearSession":
                clearChatSession(result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void handleBeginChat(@NonNull MethodCall call, @NonNull Result result) {
        final String licenseNo = call.argument("licenseNo");
        final HashMap<String, String> customParams = call.argument("customParams");
        final String groupId = call.argument("groupId");
        final String visitorName = call.argument("visitorName");
        final String visitorEmail = call.argument("visitorEmail");

        if (licenseNo == null || licenseNo.trim().isEmpty()) {
            result.error("LICENSE_ERROR", "License number cannot be empty", null);
            return;
        }

        if (activity == null) {
            result.error("ACTIVITY_ERROR", "Activity is not available", null);
            return;
        }

        try {
            ensureInitialized(licenseNo);
            LiveChat liveChat = LiveChat.getInstance();

            liveChat.setCustomerInfo(visitorName, visitorEmail, groupId, customParams);
            setupEventListeners(liveChat);

            liveChat.show(activity);
            result.success(null);
        } catch (Exception e) {
            result.error("CHAT_WINDOW_ERROR", "Failed to start chat window", e);
        }
    }

    private void ensureInitialized(String licenseNo) {
        if (initializedLicense == null || !initializedLicense.equals(licenseNo)) {
            LiveChat.initialize(licenseNo, applicationContext);
            initializedLicense = licenseNo;
        }
    }

    private void setupEventListeners(LiveChat liveChat) {
        liveChat.setNewMessageListener((message, isChatShown) -> {
            if (events != null) {
                HashMap<String, Object> messageData = new HashMap<>();
                messageData.put("EventType", "NewMessage");
                messageData.put("text", message != null ? message.getText() : null);
                messageData.put("windowVisible", isChatShown);
                events.success(messageData);
            }
        });

        liveChat.setErrorListener(cause -> {
            if (events != null) {
                HashMap<String, Object> errorData = new HashMap<>();
                errorData.put("EventType", "Error");
                errorData.put("errorDescription", cause.getMessage());
                events.success(errorData);
            }
        });

        liveChat.setUrlHandler(uri -> {
            if (events != null) {
                HashMap<String, Object> uriData = new HashMap<>();
                uriData.put("EventType", "HandleUri");
                uriData.put("uri", uri.toString());
                events.success(uriData);
            }
            return true;
        });
    }

    private void clearChatSession(Result result) {
        try {
            LiveChat.getInstance().signOutCustomer();
            result.success(null);
        } catch (IllegalStateException e) {
            result.success(null);
        }
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
        EmbeddedChatViewFactory.setActivity(binding.getActivity());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        teardownChannels();
    }

    private void teardownChannels() {
        if (methodChannel != null) {
            methodChannel.setMethodCallHandler(null);
            methodChannel = null;
        }
        if (eventChannel != null) {
            eventChannel.setStreamHandler(null);
        }
    }

    @Override
    public void onDetachedFromActivity() {
        this.activity = null;
        EmbeddedChatViewFactory.setActivity(null);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {}

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
        EmbeddedChatViewFactory.setActivity(binding.getActivity());
    }
}
