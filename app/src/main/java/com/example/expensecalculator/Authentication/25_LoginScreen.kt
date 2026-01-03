package com.example.expensecalculator.Authentication

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
import com.faiz.trekandtrack.R

val DarkBlueBackground = Color(0xFF1A387E)
val PrimaryBlue = Color(0xFF387BEE)
val LightText = Color(0xFFE0E0E0)
val DarkGreyText = Color(0xFF555555)
val DividerColor = Color(0xFFE0E0E0)
val WhiteCard = Color(0xFFFFFFFF)
val GoogleButtonBorder = Color(0xFFDCDCDC)
val HintGray = Color(0xFF8A8A8A)

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMeChecked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlueBackground)
    ) {
        // Background pattern - simulating with dots
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Color(0x22FFFFFF) // Semi-transparent white dots
            val dotRadius = 1.dp.toPx()
            val spacing = 20.dp.toPx()

            for (x in 0 until size.width.toInt() step spacing.toInt()) {
                for (y in 0 until size.height.toInt() step spacing.toInt()) {
                    drawCircle(dotColor, radius = dotRadius, center = Offset(x.toFloat(), y.toFloat()))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp)) // Top padding

            // App Logo (Assuming you have an app_logo.png in your drawable folder)
            Image(
                painter = painterResource(id = R.drawable.ic_launcher), // Replace with your actual logo
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

            // White Card for Login Fields
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Takes remaining space
                color = WhiteCard,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                shadowElevation = 8.dp // Optional: add a subtle shadow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Continue with Google Button
                    Button(
                        onClick = { /* Handle Google Login */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = DarkGreyText
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(GoogleButtonBorder))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google), // Your Google icon
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue with Google", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // "Or login with" Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = DividerColor)
                        Text(
                            text = "Or login with",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = DarkGreyText,
                            fontSize = 14.sp
                        )
                        Divider(modifier = Modifier.weight(1f), color = DividerColor)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Text Field (filled style)
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
                            unfocusedLabelColor = HintGray,
                            cursorColor = PrimaryBlue,
                            unfocusedContainerColor = Color(0xFFF0F0F0), // Light background for the field
                            focusedContainerColor = Color(0xFFF0F0F0),
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Text Field (filled style)
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
                            unfocusedLabelColor = HintGray,
                            cursorColor = PrimaryBlue,
                            unfocusedContainerColor = Color(0xFFF0F0F0),
                            focusedContainerColor = Color(0xFFF0F0F0),
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, "Toggle Password Visibility", tint = HintGray)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Remember Me & Forgot Password Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMeChecked,
                                onCheckedChange = { rememberMeChecked = it },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                            )
                            Text("Remember me", color = DarkGreyText, fontSize = 15.sp)
                        }
                        TextButton(onClick = { /* Handle Forgot Password */ }) {
                            Text("Forgot Password?", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(25.dp))

                    Button(
                        onClick = {
                            viewModel.login(context) {
                                onLoginSuccess()
                            }
                        },
                        enabled = !viewModel.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f)) // Pushes the sign up text to the bottom

                    // Sign up Text
                    val annotatedString = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = DarkGreyText, fontSize = 14.sp)) {
                            append("Don't have an account? ")
                        }
                        pushStringAnnotation(tag = "SignUp", annotation = "SignUp")
                        withStyle(style = SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
                            append("Sign Up")
                        }
                        pop()
                    }

                    ClickableText(
                        text = annotatedString,
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "SignUp", start = offset, end = offset)
                                .firstOrNull()?.let {
                                    navController.navigate("register")
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp), // Adjust padding for nav bar
                        style = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                    )
                }
            }
        }
    }
}