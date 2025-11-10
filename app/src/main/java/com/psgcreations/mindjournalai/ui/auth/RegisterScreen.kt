package com.psgcreations.mindjournalai.ui.auth

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.psgcreations.mindjournalai.R

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    context: Context,
    prefilledEmail: String = ""
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(prefilledEmail) }
    var password by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isEmailEditable = prefilledEmail.isEmpty()
    val loginSuccess by viewModel.isLoginSuccess
    val error by viewModel.errorMessage

    // Navigate to Home after registration
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            navController.navigate("journal_list") {
                popUpTo("register") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Register to start journaling",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true
                )

                // Mobile
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true
                )

                // Email (readonly if prefilled)
                OutlinedTextField(
                    value = email,
                    onValueChange = { if (isEmailEditable) email = it },
                    label = { Text("Enter Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true,
                    enabled = isEmailEditable
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Enter Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        Text(
                            text = if (passwordVisible) "Hide" else "Show",
                            modifier = Modifier
                                .clickable { passwordVisible = !passwordVisible }
                                .padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true
                )

                // Register Button
                Button(
                    onClick = { viewModel.registerUser(name, email, password, mobile) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Register")
                }

                // Already have account
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp).clickable { navController.navigate("login")  },
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(text = "Already have an account? ", color = Color.Gray, style = TextStyle(fontSize = 12.sp))
                    Text(
                        text = "Login",
                        color = MaterialTheme.colorScheme.primary,
                        style = TextStyle(fontSize = 12.sp),
                    )
                }

                // OR Divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
                    Text(
                        text = "  OR  ",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
                }

                // Google Sign-Up
                Button(
                    onClick = { viewModel.signInWithGoogle(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                    )
                    Text("Sign up with Google")
                }

                // Error Message
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

            }
        }
    }
}
