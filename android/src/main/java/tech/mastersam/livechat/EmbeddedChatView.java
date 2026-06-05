package tech.mastersam.livechat;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.activity.ComponentActivity;

import com.livechatinc.chatsdk.LiveChat;
import com.livechatinc.chatsdk.src.presentation.LiveChatView;

import java.util.Map;

import io.flutter.plugin.platform.PlatformView;

public class EmbeddedChatView implements PlatformView {

    private final LiveChatView liveChatView;

    EmbeddedChatView(Context context, Activity activity, Object args) {
        Map<String, Object> params = (Map<String, Object>) args;
        String licenseNo = (String) params.get("licenseNo");
        String groupId = (String) params.get("groupId");
        String visitorName = (String) params.get("visitorName");
        String visitorEmail = (String) params.get("visitorEmail");
        Map<String, String> customParams = (Map<String, String>) params.get("customParams");

        LiveChat.initialize(licenseNo, context.getApplicationContext());
        LiveChat.getInstance().setCustomerInfo(visitorName, visitorEmail, groupId, customParams);

        liveChatView = new LiveChatView(context, null);

        ComponentActivity componentActivity = resolveComponentActivity(activity, context);
        liveChatView.attachTo(componentActivity);
        liveChatView.init(null);
    }

    private ComponentActivity resolveComponentActivity(Activity activity, Context context) {
        if (activity instanceof ComponentActivity) {
            return (ComponentActivity) activity;
        }
        if (context instanceof ComponentActivity) {
            return (ComponentActivity) context;
        }
        throw new IllegalStateException(
                "Embedded chat requires ComponentActivity. Use FlutterFragmentActivity as your MainActivity."
        );
    }

    @Override
    public View getView() {
        return liveChatView;
    }

    @Override
    public void dispose() {
        liveChatView.clearCallbackListeners();
    }
}
