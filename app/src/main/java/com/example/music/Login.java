package com.example.music;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    private FirebaseAuth mAuth;
    EditText edtEmailLogin, edtPasswordLogin;
    Button btnLogin;
    TextView tvSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmailLogin = findViewById(R.id.edtEmailLogin);
        edtPasswordLogin = findViewById(R.id.edtPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmailLogin.getText().toString();
                String password = edtPasswordLogin.getText().toString();
                if (!email.isEmpty() && !password.isEmpty()) {
                    loginWithEmail(email, password);
                } else {
                    Toast.makeText(Login.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, SignUp.class);
                startActivity(intent);
                finish();
            }
        });

        // Kiểm tra trạng thái đăng nhập
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // Đóng Fragment2 (optional)
            return;
        }
    }

    private void changePassword(String email) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Vui lòng nhấn vào liên kết trong email của bạn để đổi mật khẩu", Toast.LENGTH_SHORT).show();
                        // Gửi OTP thành công
                        // Thực hiện các hành động tiếp theo, ví dụ: hiển thị giao diện nhập OTP
                    } else {
                        Toast.makeText(this, "Gửi email thất bại", Toast.LENGTH_SHORT).show();
                        // Gửi OTP thất bại
                        // Xử lý lỗi
                    }
                });
    }

    private void loginWithEmail(String email, String password) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Đăng nhập thành công
                        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                        // Thực hiện các hành động tiếp theo, ví dụ: chuyển đến màn hình chính
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.d("loginnn", "Đăng nhập thất bại");
                        // Đăng nhập thất bại
                        // Xử lý lỗi
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this, "Email hoặc mật khẩu không hợp lệ", Toast.LENGTH_SHORT).show();
                            // Log.d("loginnn", "Sai địa chỉ email hoặc mật khẩu");
                            // Sai địa chỉ email hoặc mật khẩu
                            // Xử lý lỗi
                        } else {
                            Toast.makeText(this, "Sai email hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                            Log.d("loginnn", "Lỗi khác: "+ task.getException().toString());
                            // Lỗi khác
                            // Xử lý lỗi
                        }
                    }
                });
    }
}