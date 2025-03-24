package com.example.music;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    private FirebaseAuth mAuth;
    EditText edtEmailSignUp, edtPasswordSignUp, edtConfirmPasswordSignUp;
    Button btnSignUp;
    TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        edtEmailSignUp = findViewById(R.id.edtEmailSignUp);
        edtPasswordSignUp = findViewById(R.id.edtPasswordSignUp);
        edtConfirmPasswordSignUp = findViewById(R.id.edtConfirmPasswordSignUp);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmailSignUp.getText().toString();
                String password = edtPasswordSignUp.getText().toString();
                String confirmPassword = edtConfirmPasswordSignUp.getText().toString();
                if (!email.isEmpty() && !password.isEmpty() && !confirmPassword.isEmpty()) {
                    if (password.equals(confirmPassword)) {
                        signUpByEmail(email, password);
                    } else {
                        Toast.makeText(SignUp.this, "Xác nhận mật khẩu không khớp", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SignUp.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUp.this, Login.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void signUpByEmail(String email, String password){
        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(emailVerificationTask -> {
                                        if (emailVerificationTask.isSuccessful()) {
                                            Toast.makeText(this, "Gửi email xác minh thành công", Toast.LENGTH_SHORT).show();
                                            // Gửi email xác minh thành công
                                            // Thực hiện các hành động tiếp theo, ví dụ: thông báo cho người dùng
                                            saveUserToDb(user, email);
                                            Intent intent = new Intent(this, Login.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(this, "Gửi email xác minh thất bại", Toast.LENGTH_SHORT).show();
                                            // Gửi email xác minh thất bại
                                            // Xử lý lỗi
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Tạo tài khoản người dùng thất bại", Toast.LENGTH_SHORT).show();
                        // Tạo tài khoản người dùng thất bại
                        // Xử lý lỗi
                    }
                });
    }

    private void saveUserToDb(FirebaseUser user, String email) {
        String username = email.split("@")[0];
        // Thêm thông tin người dùng vào collection "users"
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(user.getUid());
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("username", username);

        userRef.set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Thành công khi tạo tài liệu user trong collection "users"
                        // Hoàn thành đăng ký và thêm thông tin người dùng thành công
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Xảy ra lỗi khi tạo tài liệu user trong collection "users"
                        // Xử lý lỗi
                    }
                });
    }
}