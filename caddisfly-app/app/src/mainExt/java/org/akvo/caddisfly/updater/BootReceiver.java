package org.akvo.caddisfly.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.akvo.caddisfly.app.CaddisflyApp;

import java.util.Objects;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.requireNonNull(intent.getAction()).equals("android.intent.action.BOOT_COMPLETED")) {
            CaddisflyApp.setNextUpdateCheck(context, -1);
        }
    }
}