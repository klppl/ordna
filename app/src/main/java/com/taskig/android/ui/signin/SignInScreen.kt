package com.taskig.android.ui.signin

import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.services.tasks.TasksScopes
import com.taskig.android.R

private fun extractEmail(result: AuthorizationResult, context: android.content.Context): String? {
    result.toGoogleSignInAccount()?.email?.let { return it }

    @Suppress("DEPRECATION")
    com.google.android.gms.auth.api.signin.GoogleSignIn
        .getLastSignedInAccount(context)?.email?.let { return it }

    val accounts = AccountManager.get(context).getAccountsByType("com.google")
    return accounts.firstOrNull()?.name
}

@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val errorNoAccount = stringResource(R.string.sign_in_error_no_account)
    val errorCancelled = stringResource(R.string.sign_in_cancelled)
    val errorFailed = stringResource(R.string.sign_in_failed)

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val authResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(result.data)
            val email = extractEmail(authResult, context)
            if (email != null) {
                viewModel.onAuthSuccess(email)
            } else {
                viewModel.onAuthError(errorNoAccount)
            }
        } else {
            viewModel.onAuthError(errorCancelled)
        }
    }

    LaunchedEffect(state) {
        when (val s = state) {
            is SignInState.NeedsConsent -> {
                consentLauncher.launch(
                    IntentSenderRequest.Builder(s.pendingIntent).build()
                )
            }
            is SignInState.Success -> onSignedIn()
            is SignInState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetError()
            }
            else -> {}
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.sign_in_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.sign_in_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (state is SignInState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            } else {
                Button(
                    onClick = {
                        viewModel.setLoading()
                        val authRequest = AuthorizationRequest.builder()
                            .setRequestedScopes(listOf(
                                Scope(TasksScopes.TASKS),
                                Scope(Scopes.EMAIL),
                            ))
                            .build()

                        Identity.getAuthorizationClient(context)
                            .authorize(authRequest)
                            .addOnSuccessListener { result: AuthorizationResult ->
                                if (result.hasResolution()) {
                                    viewModel.setNeedsConsent(result.pendingIntent!!)
                                } else {
                                    val email = extractEmail(result, context)
                                    if (email != null) {
                                        viewModel.onAuthSuccess(email)
                                    } else {
                                        viewModel.onAuthError(errorNoAccount)
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                viewModel.onAuthError(e.localizedMessage ?: errorFailed)
                            }
                    },
                ) {
                    Text(stringResource(R.string.sign_in_button))
                }
            }
        }
    }
}
