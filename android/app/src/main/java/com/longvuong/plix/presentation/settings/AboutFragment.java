package com.longvuong.plix.presentation.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.longvuong.plix.BuildConfig;
import com.longvuong.plix.R;

import dagger.hilt.android.AndroidEntryPoint;

//Màn Giới thiệu: chỉ hiển thị thông tin cố định, không có trạng thái hay nghiệp vụ nên không cần ViewModel
@AndroidEntryPoint
public class AboutFragment extends Fragment {
    private static final String GITHUB_URL = "https://github.com/Nguyen-Phi-Long-702/Plix";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView textGithubLink = view.findViewById(R.id.textGithubLink);
        textGithubLink.setText(GITHUB_URL.replace("https://", ""));
        view.findViewById(R.id.rowGithub).setOnClickListener(v -> openGithub());

        TextView textAboutVersion = view.findViewById(R.id.textAboutVersion);
        textAboutVersion.setText(BuildConfig.VERSION_NAME);
    }

    private void openGithub() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Không tìm thấy ứng dụng để mở liên kết", Snackbar.LENGTH_SHORT).show();
        }
    }
}