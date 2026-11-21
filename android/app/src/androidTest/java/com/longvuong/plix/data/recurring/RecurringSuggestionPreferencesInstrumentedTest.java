package com.longvuong.plix.data.recurring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class RecurringSuggestionPreferencesInstrumentedTest {
    private static final String USER_A = "user-A";
    private static final String USER_B = "user-B";
    private static final String DISMISSED_KEY = "expense|cat-ent";

    private RecurringSuggestionPreferences preferences;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = new RecurringSuggestionPreferences(context);
        cleanUp(); //bắt đầu từ trạng thái sạch, không phụ thuộc lần chạy trước
    }

    @After
    public void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        preferences.clearForUser(USER_A);
        preferences.clearForUser(USER_B);
    }

    /**
     * TN6-I01
     * dismiss('user-A','expense|cat-ent'); đọc user-B; clearForUser('user-A'):
     * user-B không thấy khoá; sau clearForUser user-A rỗng.
     */
    @Test
    public void dismissedKeys_areIsolatedPerUser_andEmptyAfterClearForUser() {
        preferences.dismiss(USER_A, DISMISSED_KEY);

        //user-A thấy khoá vừa bỏ qua; user-B không thấy
        assertEquals(Collections.singleton(DISMISSED_KEY), preferences.getDismissedKeys(USER_A));
        assertTrue(preferences.getDismissedKeys(USER_B).isEmpty());

        //clearForUser('user-A') xoá hết khoá của user-A
        preferences.clearForUser(USER_A);
        assertTrue(preferences.getDismissedKeys(USER_A).isEmpty());
    }
}