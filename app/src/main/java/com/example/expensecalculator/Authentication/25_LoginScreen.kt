package com.example.expensecalculator.Authentication

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensecalculator.ui.theme.*
import com.faiz.trekandtrack.R
import com.google.android.gms.common.api.ApiException

// Aliases for legacy color names referenced in this file.
// These map older symbol names to the canonical colors defined in ui.theme.Color.kt
private val DarkBlueBackground = DarkScreenBackground
private val WhiteCard = CardBackground
private val DarkGreyText = PrimaryText
private val DividerColor = DividerGrey
private val HintGray = SecondaryText
private val GoogleButtonBorder = BorderGrey

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }

    // Google Sign-In launcher
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn
                .getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { token ->
                viewModel.signInWithGoogle(
                    idToken = token,
                    onSuccess = onLoginSuccess,
                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                )
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "Google sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(
            email = viewModel.email,
            onEmailChange = { viewModel.onEmailChange(it) },
            onDismiss = { showForgotPassword = false },
            onSend = {
                viewModel.forgotPassword(
                    onSuccess = {
                        Toast.makeText(context, "Reset email sent!", Toast.LENGTH_SHORT).show()
                        showForgotPassword = false
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                )
            },
            isLoading = viewModel.isLoading
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(DarkBlueBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Color(0x22FFFFFF)
            val dotRadius = 1.dp.toPx()
            val spacing = 20.dp.toPx()
            for (x in 0 until size.width.toInt() step spacing.toInt()) {
                for (y in 0 until size.height.toInt() step spacing.toInt()) {
                    drawCircle(dotColor, radius = dotRadius, center = Offset(x.toFloat(), y.toFloat()))
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_launcher),
                contentDescription = "App Logo",
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sign in to your\nAccount",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 32.sp,
                lineHeight = 40.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your email and password to log in",
                style = MaterialTheme.typography.bodyMedium,
                color = LightText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = WhiteCard,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google Sign-In Button
                    Button(
                        onClick = {
                            val client = viewModel.getGoogleSignInClient(context)
                            googleLauncher.launch(client.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = DarkGreyText
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(GoogleButtonBorder))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue with Google", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
                        Text("Or login with", modifier = Modifier.padding(horizontal = 16.dp), color = DarkGreyText, fontSize = 14.sp)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = viewModel.email,
                        onValueChange = { viewModel.onEmailChange(it) },
                        label = { Text("Email", color = HintGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = LightGray,
                            focusedLabelColor = PrimaryBlue,
                            cursorColor = PrimaryBlue,
                            unfocusedContainerColor = Color(0xFFF0F0F0),
                            focusedContainerColor = Color(0xFFF0F0F0),
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = viewModel.password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        label = { Text("Password", color = HintGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = LightGray,
                            focusedLabelColor = PrimaryBlue,
                            cursorColor = PrimaryBlue,
                            unfocusedContainerColor = Color(0xFFF0F0F0),
                            focusedContainerColor = Color(0xFFF0F0F0),
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    "Toggle Password",
                                    tint = HintGray
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showForgotPassword = true }) {
                            Text("Forgot Password?", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.login(
                                onSuccess = onLoginSuccess,
                                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                            )
                        },
                        enabled = !viewModel.isLoading,
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color.White)
                        } else {
                            Text("Log In", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val annotatedString = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = DarkGreyText, fontSize = 14.sp)) { append("Don't have an account? ") }
                        pushStringAnnotation(tag = "SignUp", annotation = "SignUp")
                        withStyle(style = SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)) { append("Sign Up") }
                        pop()
                    }
                    ClickableText(
                        text = annotatedString,
                        onClick = { offset ->
                            annotatedString.getStringAnnotations("SignUp", offset, offset)
                                .firstOrNull()?.let { navController.navigate("register") }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp),
                        style = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun ForgotPasswordDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter your email and we'll send a reset link.", fontSize = 14.sp, color = DarkGreyText)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSend, enabled = !isLoading && email.isNotBlank()) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                else Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}