package com.example.music;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OnlineArtistFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OnlineArtistFragment extends Fragment {
    private CollectionReference songsCollectionRef;
    private RecyclerView recyclerViewArtist;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public OnlineArtistFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OnlineArtistFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OnlineArtistFragment newInstance(String param1, String param2) {
        OnlineArtistFragment fragment = new OnlineArtistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_online_artist, container, false);

        songsCollectionRef = FirebaseFirestore.getInstance().collection("songs");
        recyclerViewArtist = view.findViewById(R.id.recyclerViewArtist);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerViewArtist.setLayoutManager(layoutManager);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getArtistList();
    }

    private void getArtistList() {
        songsCollectionRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                HashMap<String, Integer> artistCountMap = new HashMap<>();

                // Duyệt qua các tài liệu trong bộ sưu tập "songs"
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    // Lấy giá trị của trường "artist" từ mỗi tài liệu
                    String artist = documentSnapshot.getString("artist");
                    if (artist != null) {
                        if (artistCountMap.containsKey(artist)) {
                            int count = artistCountMap.get(artist);
                            artistCountMap.put(artist, count + 1);
                        } else {
                            artistCountMap.put(artist, 1);
                        }
                    }
                }

                Log.d("artist", String.valueOf(artistCountMap));
                // Lưu danh sách các nghệ sĩ vào Firebase hoặc thực hiện các hành động khác
                AlbumAdapter albumAdapter = new AlbumAdapter(artistCountMap, requireContext());
                recyclerViewArtist.setAdapter(albumAdapter);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Xử lý khi có lỗi xảy ra
            }
        });
    }
}