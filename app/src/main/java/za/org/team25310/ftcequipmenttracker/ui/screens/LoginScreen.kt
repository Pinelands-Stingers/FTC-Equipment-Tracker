package za.org.team25310.ftcequipmenttracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import za.org.team25310.ftcequipmenttracker.ui.AuthViewModel

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val user by viewModel.user.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val emailVerified by viewModel.emailVerified.collectAsState()

    LaunchedEffect(user, emailVerified) {
        if (user != null && emailVerified) onLoggedIn()
    }

    val currentUser = user
    if (currentUser != null && !emailVerified) {
        VerifyEmailScreen(
            email = currentUser.email,
            loading = loading,
            error = error,
            onResend = { viewModel.resendVerification() },
            onContinue = { viewModel.refreshVerification() },
            onSignOut = { viewModel.signOut() },
        )
    } else {
        SignInForm(
            loading = loading,
            error = error,
            onSignIn = { email, password -> viewModel.signIn(email, password) },
            onSignUp = { email, password -> viewModel.signUp(email, password) },
        )
    }
}

@Composable
private fun SignInForm(
    loading: Boolean,
    error: String?,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("FTC Equipment Tracker", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("by Team #25310 Pinelands Stingers", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (isSignUp) onSignUp(email, password) else onSignIn(email, password)
                },
                enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (loading) "Please wait..." else if (isSignUp) "Sign up" else "Sign in")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(if (isSignUp) "Already have an account? Sign in" else "New team? Create an account")
            }
        }
    }
}

@Composable
private fun VerifyEmailScreen(
    email: String?,
    loading: Boolean,
    error: String?,
    onResend: () -> Unit,
    onContinue: () -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Verify your email", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text(
                "We sent a verification link to ${email ?: "your email"}. " +
                    "Open the link, then tap the button below to continue.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(24.dp))

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = onContinue,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (loading) "Checking..." else "I've verified my email")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onResend,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Resend verification email")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSignOut) {
                Text("Use a different account")
            }
        }
    }
}
