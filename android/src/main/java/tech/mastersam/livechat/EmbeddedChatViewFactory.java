package tech.mastersam.livechat;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class EmbeddedChatViewFactory extends PlatformViewFactory {

    @Nullable
    private static Activity activity;

    public EmbeddedChatViewFactory() {
        super(StandardMessageCodec.INSTANCE);
    }

    static void setActivity(@Nullable Activity activity) {
        EmbeddedChatViewFactory.activity = activity;
    }

    @Override
    public PlatformView create(@NonNull Context context, int id, Object args) {
        return new EmbeddedChatView(context, activity, args);
    }
}
