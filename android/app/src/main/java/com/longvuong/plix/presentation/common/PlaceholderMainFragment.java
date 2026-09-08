package com.longvuong.plix.presentation.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;

public class PlaceholderMainFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        TextView textView = new TextView(getContext());
        textView.setPadding(48, 48, 48, 48);
        return textView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavDestination currentDestination = Navigation.findNavController(view).getCurrentDestination();
        CharSequence label = currentDestination != null ? currentDestination.getLabel() : null;

        ((TextView) view).setText(label != null ? label + " (placeholder)" : "Placeholder");
    }
}