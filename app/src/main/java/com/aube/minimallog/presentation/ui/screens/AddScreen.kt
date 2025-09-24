package com.aube.minimallog.presentation.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aube.minimallog.R
import com.aube.minimallog.presentation.model.MemoryDraft
import com.aube.minimallog.presentation.ui.component.TagInput
import com.aube.minimallog.presentation.viewmodel.MemoryViewModel
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

// ---- Public API --------------------------------------------------------------

@Composable
fun AddScreen(
    modifier: Modifier = Modifier,
    onSavedNavigateBack: () -> Unit,
    vm: MemoryViewModel = hiltViewModel()
) {
    // State
    var id by rememberSaveable { mutableStateOf<Long?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var isFavorite by rememberSaveable { mutableStateOf(false) }
    var tags by rememberSaveable { mutableStateOf(listOf<String>()) }
    var isFocused by remember { mutableStateOf(false) }
    var prefilled by remember { mutableStateOf(false) }

    val item by vm.editTarget.collectAsState()

    val cropLauncher = rememberLauncherForActivityResult(
        CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { croppedUri ->
                imageUri = croppedUri
            }
        }
    }

    val cropRequest = CropImageContractOptions(
        uri = null,
        cropImageOptions = CropImageOptions().apply {
            imageSourceIncludeCamera = true
            imageSourceIncludeGallery = true
            fixAspectRatio = true
            aspectRatioX = 1
            aspectRatioY = 1
        }
    )

    LaunchedEffect(Unit) {
        vm.events.collectLatest { ev ->
            when (ev) {
                is MemoryViewModel.Event.Saved -> {
                    // TODO 스낵바 등
                    onSavedNavigateBack()
                }
                is MemoryViewModel.Event.Error -> {
                    // TODO 스낵바 등
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(item?.id) {
        if (item != null && !prefilled) {
            id = item!!.id
            title = item!!.title
            description = item!!.description
            imageUri = item!!.imageUrl?.let { pathOrUrl ->
                // 로컬 파일 경로면 file:// Uri로, 아니면 URL/Content Uri로
                if (pathOrUrl.startsWith("/")) Uri.fromFile(java.io.File(pathOrUrl))
                else Uri.parse(pathOrUrl)
            }
            date = item!!.date
            isFavorite = item!!.isFavorite
            tags = item!!.tags
            prefilled = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // 고정 하단 액션바
            Surface(tonalElevation = 2.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // 제스처바 영역 피하기
                        .imePadding()             // 키보드 올라올 때 버튼 위로
                        .background(
                            if(title.isBlank()) MaterialTheme.colorScheme.primaryContainer.copy(0.6f)
                            else MaterialTheme.colorScheme.primary
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.titleMedium,
                        color =
                            if(title.isBlank()) MaterialTheme.colorScheme.onSecondary.copy(0.6f)
                            else MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                if (title.isNotBlank()) {
                                    vm.save(
                                        MemoryDraft(
                                            id = id,
                                            title = title.trim(),
                                            description = description.trim(),
                                            imageUri = imageUri,
                                            date = date,
                                            tags = tags,
                                            isFavorite = isFavorite
                                        )
                                    )
                                }
                            }
                    )
                }
            }
        }
    ) { innerPadding ->
        // 스크롤되는 본문
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 이미지
            ImagePickerCard(
                imageUri = imageUri,
                onPick = { cropLauncher.launch(cropRequest) },
                onClear = { imageUri = null }
            )

            // 제목
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                placeholder = { Text(stringResource(R.string.placeholder_memory_title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                supportingText = {
                    Text(
                        text = "${title.length} / 80",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.End
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                )
            )

            // 설명
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                placeholder = { Text(stringResource(R.string.placeholder_memory_description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                textStyle = MaterialTheme.typography.bodyMedium,
                minLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default),
                supportingText = {
                    Text(
                        text = "${description.length} / 500",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.End
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                )
            )

            // 태그
            TagInput(
                tags = tags,
                onFocusChanged = { isFocused = it },
                onTagAdd = { newTag ->
                    if (newTag.isNotBlank() && newTag !in tags && tags.size < 10)
                    tags = tags + newTag
                },
                onTagRemove = { tag -> tags = tags - tag }
            )

            Spacer(Modifier.height(40.dp)) // 마지막 입력 뒤 공간(하단바와 겹침 방지용)
        }
    }
}

// ---- Sub-composables ---------------------------------------------------------

@Composable
private fun ColumnScope.ImagePickerCard(
    imageUri: Uri?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3EE)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onPick() }
    ) {
        Box(
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(1f)
        ) {
            if (imageUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.text_select_picture))
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = imageUri.toString(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                // Clear button
                Icon(
                    tint = MaterialTheme.colorScheme.onBackground,
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_delete),
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onClear() }
                        .padding(8.dp)
                )
            }
        }
    }
}
