package com.longvuong.plix;

import android.os.Bundle;
import android.view.View;
import android.Manifest;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.notification.NotificationHelper;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    @Inject
    AuthManager authManager;

    @Inject
    NotificationHelper notificationHelper;

    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;
    private NavController navController;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        //Chỉ hiện thanh điều hướng dưới khi đang ở trong mainGraph
        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                updateBottomNavVisibility(destination));

        authManager.getSessionExpiredLiveData().observe(this, expired -> {
            if (Boolean.TRUE.equals(expired)) {
                navController.navigate(
                        R.id.authGraph,
                        null,
                        new NavOptions.Builder().setPopUpTo(0, true).build());
                authManager.onSessionExpiredHandled();
            }
        });

        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> notificationHelper.onPermissionRequestHandled());

        notificationHelper.getPermissionRequestNeededLiveData().observe(this, needed -> {
            if (Boolean.TRUE.equals(needed) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });
    }

    private void updateBottomNavVisibility(NavDestination destination) {
        boolean inMainGraph = isInGraph(destination, R.id.mainGraph);
        bottomNavigationView.setVisibility(inMainGraph ? View.VISIBLE : View.GONE);
    }

    private boolean isInGraph(NavDestination destination, int graphId) {
        NavDestination current = destination;
        while (current != null) {
            if (current.getId() == graphId) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}