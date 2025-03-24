package com.example.music;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AccountFragment extends Fragment {
    private Button btnSignOut, btnChangePW;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private ImageView imageViewAvatar, imageViewEdit;
    private EditText editTextName;
    private TextView textViewName;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AccountFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AccountFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AccountFragment newInstance(String param1, String param2) {
        AccountFragment fragment = new AccountFragment();
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
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        btnSignOut = view.findViewById(R.id.btnSignOut);
        btnChangePW = view.findViewById(R.id.btnChangePW);
        imageViewAvatar = view.findViewById(R.id.imageViewAvatar);
        imageViewEdit = view.findViewById(R.id.imageViewEdit);
        textViewName = view.findViewById(R.id.textViewName);
        editTextName = view.findViewById(R.id.editTextName);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        displayAvatar(user.getUid());
        displayUsername(user.getUid());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                // Thực hiện các hành động sau khi đăng xuất thành công, ví dụ: chuyển đến màn hình đăng nhập
                Intent intent = new Intent(requireContext(), Login.class);
                startActivity(intent);
                requireActivity().finish();
                return;
            }
        });

        btnChangePW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

        imageViewAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectAndUploadImage();
            }
        });

        imageViewEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textViewName.setVisibility(View.GONE);
                imageViewEdit.setVisibility(View.GONE);
                editTextName.setText(textViewName.getText().toString());
                editTextName.setVisibility(View.VISIBLE);
                editTextName.requestFocus();
            }
        });

        editTextName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    // Người dùng rời khỏi EditText, lưu username
                    String newUsername = editTextName.getText().toString();
                    editTextName.setVisibility(View.GONE);
                    textViewName.setText(newUsername);
                    textViewName.setVisibility(View.VISIBLE);
                    imageViewEdit.setVisibility(View.VISIBLE);
                    updateUsernameInFirestore(newUsername);
                }
            }
        });
    }

    private void changePassword() {
        if (user != null) {
            String email = user.getEmail();

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Vui lòng nhấn vào liên kết trong email của bạn để đổi mật khẩu", Toast.LENGTH_SHORT).show();
                            // Gửi OTP thành công
                            // Thực hiện các hành động tiếp theo, ví dụ: hiển thị giao diện nhập OTP
                        } else {
                            Toast.makeText(requireContext(), "Gửi email thất bại", Toast.LENGTH_SHORT).show();
                            // Gửi OTP thất bại
                            // Xử lý lỗi
                        }
                    });
        }
    }

    private void updateUsernameInFirestore(String newUsername) {
        if (user != null) {
            String userId = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.update("username", newUsername)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Thành công khi cập nhật username trong Firestore
                            // Xử lý thành công
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Xảy ra lỗi khi cập nhật username trong Firestore
                            // Xử lý lỗi
                        }
                    });
        }
    }

    private void selectAndUploadImage() {
        // Mở trình chọn ảnh
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), 999);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 999 && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            // Lấy Uri của ảnh đã chọn
            Uri imageUri = data.getData();

            // Hiển thị ảnh đã chọn lên ImageView
            imageViewAvatar.setImageURI(imageUri);

            // Tải ảnh lên Firebase Storage
            uploadImageToFirebaseStorage(user.getUid(), imageUri);
        }
    }

    private void uploadImageToFirebaseStorage(String userId, Uri imageUri) {
        // Tạo tham chiếu đến Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference avatarRef = storageRef.child("avatars").child(userId);

        // Tải ảnh lên Firebase Storage
        UploadTask uploadTask = avatarRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Lấy đường dẫn tới ảnh vừa tải lên
                avatarRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String imageUrl = uri.toString();

                        // Cập nhật đường dẫn ảnh vào Firestore
                        updateAvatarInFirestore(userId, imageUrl);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Xảy ra lỗi khi tải ảnh lên Firebase Storage
                // Xử lý lỗi
            }
        });
    }

    private void updateAvatarInFirestore(String userId, String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);

        HashMap<String, Object> userData = new HashMap<>();
        userData.put("avatar", imageUrl);

        userRef.set(userData, SetOptions.merge()) // SetOptions.merge() đảm bảo rằng các giá trị hiện có trong tài liệu người dùng không bị ghi đè, và chỉ trường "avatar" sẽ được tạo mới hoặc cập nhật nếu nó chưa tồn tại
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Thành công khi cập nhật đường dẫn ảnh vào Firestore
                        // Xử lý thành công
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Xảy ra lỗi khi cập nhật đường dẫn ảnh vào Firestore
                        // Xử lý lỗi
                    }
                });
    }

    private void displayAvatar(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String avatarUrl = documentSnapshot.getString("avatar");
                        if (avatarUrl != null) {
                            // Hiển thị ảnh avatar
                            Picasso.get().load(avatarUrl).into(imageViewAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi khi truy vấn Firestore
                });
    }

    private void displayUsername(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        textViewName.setText(username);
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi khi truy vấn Firestore
                });
    }
}