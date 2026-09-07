package com.longvuong.plix.core.network;

import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NetworkObserverTest {

    @Test
    public void construct_doesNotCrash_andExposesLiveData() {
        Context context = ApplicationProvider.getApplicationContext();

        NetworkObserver networkObserver = new NetworkObserver(context);

        assertNotNull(networkObserver.getIsConnected());
    }
}