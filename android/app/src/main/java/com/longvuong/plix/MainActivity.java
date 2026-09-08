package com.longvuong.plix;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private NavController navController;
    private BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                updateBottomNavVisibility(destination));
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