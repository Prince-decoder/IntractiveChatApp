package com.my.forintern.OnBoarding

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toLong
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.my.forintern.Screens
import com.my.forintern.UserRoomDataBase.UserDATASET
import com.my.forintern.UserRoomDataBase.UserViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navhost: NavController,
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
    userViewModel: UserViewModel
) {
    val context= LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val onboardingState by viewModel.onboardingState.collectAsState()

    // We use a local state for OTP since it's just for validation in step 2 and not part of the persistent profile
    var otp by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                if(pagerState.currentPage>0)
                {
                    Button(modifier = Modifier.wrapContentSize(),onClick = {
                        coroutineScope.launch {
                            val currentpage= pagerState.currentPage
                            pagerState.animateScrollToPage(currentpage-1);
                        }
                    }) {
                        Text("Back")
                    }
                }
                Button(
                    modifier =if(pagerState.currentPage==0) Modifier.fillMaxWidth() else Modifier.wrapContentSize(),
                    onClick = {
                        coroutineScope.launch {
                            val currentPage = pagerState.currentPage
                            if (currentPage == 0) {
                                pagerState.animateScrollToPage(1)
                            } else if (currentPage == 1) {
                                if (viewModel.isStep2Valid(onboardingState, otp)) {
                                    pagerState.animateScrollToPage(2)
                                } else {
                                    if(!otp.equals("1234"))
                                    {
                                        Toast.makeText(context,"Wrong OTP",Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else if (currentPage == 2) {
                                if (viewModel.isStep3Valid(onboardingState)) {
                                    viewModel.saveProfile()
                                    onOnboardingComplete()
                                    userViewModel.addOrUpdateUserKeepHistory(
                                        UserDATASET(
                                            onboardingState.phone.toLong(), onboardingState.name,
                                            listOf()
                                        )
                                    )
                                    navhost.navigate(Screens.HomeScreen.route.replace("{username}", onboardingState.name))
                                }
                            }
                        }
                    },
                    enabled = when (pagerState.currentPage) {
                        0 -> true
                        1 -> otp.isNotBlank()
                        2 -> viewModel.isStep3Valid(onboardingState)
                        else -> true
                    }
                ) {
                    Text(if (pagerState.currentPage == 2) "Finish" else "Next")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "background")
            val offset1 by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(10000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset1"
            )
            val offset2 by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset2"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawCircle(
                    color = Color(0x1A6200EA), // 10% opacity
                    center = Offset(w * (0.2f + 0.6f * offset1), h * (0.2f + 0.2f * offset2)),
                    radius = w * 0.7f
                )

                drawCircle(
                    color = Color(0x1A03DAC5), // 10% opacity
                    center = Offset(w * (0.8f - 0.4f * offset2), h * (0.8f - 0.2f * offset1)),
                    radius = w * 0.8f
                )

                drawCircle(
                    color = Color(0x1ABB86FC), // 10% opacity
                    center = Offset(w * (0.5f + 0.3f * offset2), h * (0.5f + 0.3f * offset1)),
                    radius = w * 0.6f
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> Step1ValueProps()
                    1 -> Step2Form(
                        name = onboardingState.name,
                        age = onboardingState.age,
                        phone = onboardingState.phone,
                        otp = otp,
                        onNameChange = viewModel::updateName,
                        onAgeChange = viewModel::updateAge,
                        onPhoneChange = viewModel::updatePhone,
                        onOtpChange = { otp = it }
                    )
                    2 -> Step3Personality(
                        selectedTraits = onboardingState.traits,
                        onTraitToggle = viewModel::toggleTrait
                    )
                }
            }
        }
    }
}