package com.simats.chacolateprinter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignInScreen(
    onSignInSuccess: (String, String) -> Unit,
    onCreateAccountClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF5D4037),
            Color(0xFF3E2723),
            Color(0xFF1B0000)
        )
    )

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Rotating Cube (Central Animation)
            RotatingCube(modifier = Modifier.size(70.dp))

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "ChocoPrint 3D",
                color = Color(0xFFFFC107),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Professional Chocolate Printing",
                color = Color(0xFFA1887F),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3E2723).copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5D4037)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign In",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = Color(0xFFA1887F)) },
                        placeholder = { Text("Enter email", color = Color(0xFF5D4037)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFFFC107)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFC107),
                            unfocusedBorderColor = Color(0xFF5D4037),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFFFC107)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color(0xFFA1887F)) },
                        placeholder = { Text("Enter password", color = Color(0xFF5D4037)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFC107)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFC107),
                            unfocusedBorderColor = Color(0xFF5D4037),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFFFC107)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { onSignInSuccess(email, password) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFAB00),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(onClick = onForgotPasswordClick) {
                        Text("Forgot password?", color = Color(0xFFFFC107))
                    }
                    
                    HorizontalDivider(color = Color(0xFF5D4037), thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
                    
                    Text(
                        text = "Don't have an account?",
                        color = Color(0xFFA1887F),
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = onCreateAccountClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5D4037)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Footer Text (Positioned slightly higher)
            Text(
                text = "Powered by SIMATS Engineering",
                color = Color(0xFFA1887F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
